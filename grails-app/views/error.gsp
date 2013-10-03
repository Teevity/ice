<html>
    <head>
        <title><g:if env="development">Ice Runtime Exception</g:if><g:else>Error</g:else></title>
        <g:if env="development"><link rel="stylesheet" href="${resource(dir: 'css', file: 'errors.css')}" type="text/css"></g:if>
    </head>
    <body>
        <g:if env="development">
            <g:renderException exception="${exception}" />
        </g:if>
        <g:else>
            <ul class="errors">
                <li>An error has occurred</li>
            </ul>
        </g:else>
    </body>
</html>
