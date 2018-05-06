<!DOCTYPE html>
<html lang="en">
    <% boolean blah = false; %>
    <% if(blah) {
        int str = 0;
        %>
        <%@ section shouldBeInt(str) %>
        ${str + 1}
        <%@ end section %>
    <% } else {
        String str = "i am a int str wow"; %>
        <%@ section mySection(str) %>
            <h1> urmom</h1>
        <%@ end section %>
    <% } %>
    <%@ section shouldBeABoolean(blah) %>
    <h1>${blah}</h1>
    <%@ end section %>
</html>
<%-- What's up this is a comment --%>