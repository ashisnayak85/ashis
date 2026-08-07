package test;
import javax.servlet.*;
import java.io.*;
import javax.servlet.annotation.*;
import javax.servlet.http.*;
@SuppressWarnings("serial")
@WebServlet("/logout")
public class AdminLogOut extends HttpServlet {
@Override
public void doGet(HttpServletRequest req,HttpServletResponse res) throws IOException,ServletException
{
	Cookie[] c=req.getCookies();
	
	if(c==null)
	{
		req.setAttribute("msg","Session expired<br>");
	}
	else
	{
		ServletContext sct=req.getServletContext();
//		Acessing Servlet Context object
		sct.removeAttribute("abean");
		c[0].setMaxAge(0);
		res.addCookie(c[0]);
		req.setAttribute("msg", "LogOut Sucessfully<br>");
	}
	RequestDispatcher rd=req.getRequestDispatcher("Msg.jsp");
	rd.forward(req, res);
}
}
