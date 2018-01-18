<html>
<head><title>First JSP</title></head>
<body>
<%
double num = Math.random();
if (num > 0.95) {
%>
<h2>You'll have a luck day!</h2><p>( <%= num %> )</p>
<%
} else {
%>
<h2>Well, life goes on ... </h2><p>( <%= num %> )</p>
<%
}
%>
<a href="www.facebook.com"><h3>Try Again</h3></a>

    <%@ section MySection(num) %>
    <%@ import java.util.* %>
        <body>
            <h1>This is a demo template</h1>
            <p>1 + 1 = ${1 + 1}</p>
        </body>
    <%@ section yourSection(num) %>
        <body>
        <% int fontSize; %>
        <%for ( fontSize = 1; fontSize <= 3; fontSize++){ %>
        <font color = "green" size = "<%= fontSize %>">
            JSP Tutorial
        </font><br />
        <%}%>
        </body>
    <%@ end section %>
    <%@ end section %>

</body>

</html>
