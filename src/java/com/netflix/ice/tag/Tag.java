/*
 *
 *  Copyright 2013 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
package com.netflix.ice.tag;

import java.io.Serializable;

public abstract class Tag implements Comparable<Tag>, Serializable {
    public static final Tag aggregated = new Tag("aggregated") {
        @Override
        public int compareTo(Tag t) {
            return this == t ? 0 : -1;
        }
    };

    public final String name;
    public final String s3Name;
    Tag(String name) {
        this.name = name;
        this.s3Name = Tag.toS3(name);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Tag)
            return this.name.equals(((Tag)o).name);
        else
            return false;
    }

    /**
    * Normalize a tagname suitable to be an S3 Filename
    */
    public static String toS3(String name) {
        name = name.replaceAll("/","--");
        return name;
    }

    /**
    * Normalize a tagname from an S3 Filename
    */
    public static String fromS3(String name) {
        name = name.replaceAll("--","/");
        return name;
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    public int compareTo(Tag t) {
        if (t == aggregated)
            return -t.compareTo(this);
        int result = ("a" + this.name).compareTo("a" + t.name);
        return result;
    }
}
