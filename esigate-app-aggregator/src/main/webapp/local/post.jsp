<%@page contentType="text/html; charset=UTF-8"%>
<%request.setCharacterEncoding("UTF-8"); %>
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>Form post example</title>
</head>
<body style="background-color: yellow">
<% if ("POST".equals(request.getMethod())) { %>
Method = POST<br />
Posted field value = <%=request.getParameter("myField") %>
<% } else { %>
<form method="post">
<input type="text" name="myField" />
<input type="submit" name="send" value="Post this form"/>
</form>
<% } %>
</body>
</html>