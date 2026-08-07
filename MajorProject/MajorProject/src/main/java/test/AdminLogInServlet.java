package test;
import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
@SuppressWarnings("serial")
@WebServlet("/adminLogIn")
public class AdminLogInServlet extends HttpServlet {
@Override
public void doPost(HttpServletRequest req,HttpServletResponse res)throws IOException,ServletException
{
	AdminBean ab=new AdminLogInDAO().login(req);
	
	if(ab==null)
	{
		req.setAttribute("msg","Invalid LogIn Process<br>");
		RequestDispatcher rd=req.getRequestDispatcher("Msg.jsp");
		rd.forward(req, res);
	}
	else
	{

		Cookie ck=new Cookie("fname",ab.getuName());
		ServletContext sct=req.getServletContext();
//		Acessing servletContext Object reference
		sct.setAttribute("abean", ab);
//		Adding attribute to servlet;
		res.addCookie(ck);
//		Adding cookie Object in response
		RequestDispatcher rd=req.getRequestDispatcher("Login.jsp");
		rd.forward(req, res);
	}
}
}
