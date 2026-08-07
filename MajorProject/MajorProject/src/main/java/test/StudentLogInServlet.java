package test;
import java.io.*;
import javax.servlet.http.*;
import javax.servlet.*;
import javax.servlet.annotation.*;
@SuppressWarnings("serial")
@WebServlet("/studentLogIn")
public class StudentLogInServlet extends HttpServlet{
@Override
public void doPost(HttpServletRequest req,HttpServletResponse res) throws IOException,ServletException
{
	StudentInfoBean sb=new StudentLogInDAO().login(req);
	if(sb==null)
	{
		req.setAttribute("msg","Invalid LogIn Process<br>");
		RequestDispatcher rd=req.getRequestDispatcher("Msg.jsp");
		rd.forward(req, res);
	}
	else
	{

		Cookie ck=new Cookie("fname",sb.getName());
		ServletContext sct=req.getServletContext();
//		Acessing servletContext Object reference
		sct.setAttribute("sbean", sb);
//		Adding attribute to servlet;
		res.addCookie(ck);
//		Adding cookie Object in response
		RequestDispatcher rd=req.getRequestDispatcher("Login.jsp");
		rd.forward(req, res);
	}
}
}
