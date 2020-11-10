<%@page session="false" pageEncoding="utf-8" %>
<style type="text/css">
    #force-login {
        position: absolute;
        top: 0;
        bottom: 0;
        left: 0;
        right: 0;
        background: #eee;
    }

    #force-login,
    #force-login input,
    #force-login button {
        font-family: sans-serif;
        font-size: 16px;
    }

    #force-login .modal-header button,
    #force-login .modal-footer .logout,
    #force-login .modal-footer .cancel {
        display: none;
    }

    #force-login .modal-content {
        width: 400px;
        margin: 100px auto;
    }

    #force-login .form-group {
        margin: 5px 0;
    }

    #force-login .form-group label {
        display: inline-block;
        width: 80px;
        margin-right: 10px;
    }

    #force-login .form-group input {
        width: 200px;
        margin-right: 10px;
        border: 1px solid #666;
    }

    #force-login .modal-footer .login {
        width: 100px;
        margin: 10px 0 10px 193px;
        border: 1px solid #666;
    }
</style>
<script type="application/javascript">
    document.addEventListener('DOMContentLoaded', function () {
        if (!window.core || typeof window.core.getWidget !== 'function') {
            var loginDlg = document.getElementById('user-status-dialog');
            var ui = document.getElementById('ui');
            var loginForm = loginDlg.innerHTML;
            ui.innerHTML = '<div id="force-login">' + loginForm + '</div>';
            var forceLogin = document.getElementById('force-login');
            var content = document.querySelector('#force-login .modal-content');
            content.innerHTML = "<h3>Console assets not accessible...</h3>"
                    + content.innerHTML
                    + "<p>Check the ACLs. The assets located in<ul><li>'/lib/jslibs' and</li><li>'/libs/composum'</li></ul>must be accessible.</p>"
                    + "<p>Alternatively sign in with appropriate credentials.</p></div>";
            var formContent = document.querySelector('#force-login .modal-content .modal-body');
            formContent.innerHTML = '<input type="hidden" name="resource" value="' + document.URL + '"/>'
                    + formContent.innerHTML;
        }
    });
</script>