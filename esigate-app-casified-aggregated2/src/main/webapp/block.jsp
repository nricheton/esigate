<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
<title>Page containing blocks</title>
</head>
<body style="background-color: aqua">
<div>Begin page</div>
<div style="border: 1px solid red">

<!--$beginblock$myblock$-->
	<div style="background-color: aqua">
	This is a block from aggregated2
	<br />
	User: <%=request.getRemoteUser() %>
	</div>
<!--$endblock$myblock$-->
</div>
</body>
</html>