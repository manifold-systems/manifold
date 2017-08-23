<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Nested Import Tests</title>
</head>
<body>
    <h1>This will make sure that nested imports are handled correctly.</h1>
    <%@ section mySection %>
        <%@ import java.util.* %>
        <% HashSet<Integer> myHashSet = new HashSet<>();
        myHashSet.add(1);
        myHashSet.add(2);
        myHashSet.add(3);
        for(Integer a: myHashSet) { %>
        <h2 style="font-size: ${a}">Font size: ${a}</h2>
        <% } %>
    <%@ end section %>
        <p> The above section should work </p>
</body>
</html>