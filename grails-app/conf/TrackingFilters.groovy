/*
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.util.regex.Pattern
import org.joda.time.DateTime

class TrackingFilters {

    static final Pattern nonBrowserUserAgents = Pattern.compile(
            '.*(libcurl|Python-urllib|Wget|HttpClient|lwp-request|Java).*')

    static filters = {
        allExceptError(controller: 'error', invert: true) {
            before = {

                String userAgent = request.getHeader('user-agent')
                if (userAgent?.contains('MSIE') && !userAgent.contains('chromeframe')) {
                    request['ieWithoutChromeFrame'] = true
                }
            }
        }
    }
}
