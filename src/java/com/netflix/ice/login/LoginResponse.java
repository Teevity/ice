/*
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
package com.netflix.ice.login;

import java.io.File;
import java.util.Map;
import java.util.Date;

/**
* Simple Response Object directs the Login Controller how to handle
* the login request.
*/
public class LoginResponse
{
    /**Did the handler respond to this(no action from the Controller) */
    public boolean responded=false;

    /** Was the Login Successful */
    public boolean loginSuccess=false;

    /** Did the Login Fail */
    public boolean loginFailed=false;

    /** Did we log the user out */
    public boolean loggedOut=false;

    /** Re-direct to a controller */
    public String redirectTo=null;

    /** A template File to render  */
    public File templateFile=null;

    /** Raw data to render  */
    public String renderData=null;

    /** templateFile or renderData mime-type */
    public String contentType=null;

    /** Variables to pass to templateFile or renderData */
    public Map<String,String> templateBindings;

    public String toString() {
        return "LoginResponse [" +
                "responseHandled: " + responded + ", " +
                "loginSuccess: " + loginSuccess + ", " +
                "loginFailed: " + loginFailed + ", " +
                "loggedOut: " + loggedOut + ", " +
                "redirectTo: " + redirectTo + ", " +
                "renderData: " + renderData + ", " +
                "contentType: " + contentType + ", " +
                "TemplateFile: " + templateFile + ", " +
                "]";
    }
}
