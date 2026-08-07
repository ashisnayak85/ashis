package test;
import java.io.*;
import javax.servlet.*;
import javax.servlet.annotation.*;
import javax.servlet.http.*;
@SuppressWarnings("serial")
@WebServlet("/reg")
public class StudentInfoServlet extends HttpServlet{
@Override
protected void doPost(HttpServletRequest req,HttpServletResponse res) throws IOException,ServletException
{
	PrintWriter pw=res.getWriter();
	res.setContentType("text/html");
	StudentInfoBean sib=new StudentInfoBean();
	sib.setRegNo(req.getParameter("regNo"));
	sib.setName(req.getParameter("name"));
	sib.setBranch(req.getParameter("branch"));
	sib.setaYear(Integer.parseInt(req.getParameter("aYear")));
	sib.setpYear(Integer.parseInt(req.getParameter("pYear")));
	sib.settAmt(Float.parseFloat(req.getParameter("tAmt")));
	sib.setrAmt(Float.parseFloat(req.getParameter("rAmt")));
	sib.setEmail(req.getParameter("email"));
	sib.setPhNo(Long.parseLong(req.getParameter("phNo")));
	
	int k=new StudentInfoDAO().insert(sib);
//	pw.println(k);
	if(k>0)
	{
		req.setAttribute("msg","Data Inserted Sucessfully...<br>");
		
	}
	else
	{
		req.setAttribute("msg","Invalid data <br> try again...");
	}
	RequestDispatcher rd=req.getRequestDispatcher("Msg.jsp");
	rd.forward(req, res);
}
}
