#!/bin/bash

# Exit the script if an error occur
set -e

# Get Ice in a ready-to-run state on a fresh AWI instance.

GRAILS_VERSION=2.4.4

# Install prerequisites
if [ -f /etc/redhat-release ]; then
    echo "Installing redhat packages"
    sudo yum -y install git java-1.7.0-openjdk-devel.x86_64 wget unzip

elif [[ -f /etc/debian-release || -f /etc/debian_version ]];then
    echo "Installing debian packages"
    sudo apt-get -y install git openjdk-7-jdk wget unzip

elif [[ -f /etc/issue && $(grep "Amazon Linux AMI" /etc/issue) ]]; then
    echo "Assuming AWS AMI, installing packages"
    sudo yum -y install git java-1.7.0-openjdk-devel.x86_64 wget unzip

else 
    echo "Unknown operating system. You may have to install Java 7 manually."
fi

INSTALL_DIR=$(pwd)

cd

HOME_DIR=$(pwd)

# Prep grails in such a way that we only download it once
if [ ! -x ".grails/wrapper/${GRAILS_VERSION}/grails-${GRAILS_VERSION}" ]; then
  mkdir -p .grails/wrapper/
  cd .grails/wrapper/
  # (Fetch)
  wget http://dist.springframework.org.s3.amazonaws.com/release/GRAILS/grails-${GRAILS_VERSION}.zip -O grails-${GRAILS_VERSION}-download.zip
  mkdir ${GRAILS_VERSION}
  cd ${GRAILS_VERSION}
  # ("Install")
  unzip ../grails-${GRAILS_VERSION}-download.zip
fi

GRAILS_HOME=${HOME_DIR}/.grails/wrapper/${GRAILS_VERSION}/grails-${GRAILS_VERSION}/
PATH=$PATH:${HOME_DIR}/.grails/wrapper/${GRAILS_VERSION}/grails-${GRAILS_VERSION}/bin/

# Get ice
cd ${INSTALL_DIR}

if [ -x '.git' ]; then
  # We already have it; update to latest git
  git pull
else
  # We don't have it at all yet; clone the repo
  git clone https://github.com/Netflix/ice.git
  cd ice
  INSTALL_DIR=$(pwd)
fi

# Initialize Ice with Grails
grails ${JAVA_OPTS} wrapper

# (Bug: Ice can't deal with this file existing and being empty.)
rm -f grails-app/i18n/messages.properties

# Create our local work directories (both for processing and reading)
mkdir -p ${HOME_DIR}/ice_processor
mkdir -p ${HOME_DIR}/ice_reader

# Set up the config file
cp src/java/sample.properties src/java/ice.properties
echo Please enter the name of the bucket Ice will read Amazon billing information from:
while [ "$BILLBUCKET" == "" ]
do
  echo -n "-> "
  read -r BILLBUCKET
done

echo Please enter the name of the bucket Ice will write processed billing information to:
while [ "$PROCBUCKET" == "" ]
do
  echo -n "-> "
  read -r PROCBUCKET
done
sed -ri 's/=billing_s3bucketprefix\//=/; s|\/mnt\/|'"${HOME_DIR}"'\/|; s/=work_s3bucketprefix\//=/; s/^ice.account.*//; s/=billing_s3bucketname1/='${BILLBUCKET}'/; s/=work_s3bucketname/='${PROCBUCKET}'/' src/java/ice.properties

echo Ice is now ready to run as a processor. If you want to run the reader, edit:
echo "${INSTALL_DIR}/src/java/ice.properties"
echo and alter the appropriate flags. You can now start Ice by running the following from the Ice root directory:
echo ./grailsw -Djava.net.preferIPv4Stack=true -Dice.s3AccessKeyId=\<access key ID\> -Dice.s3SecretKey=\<access key\> run-app
