package test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.http.HttpServletRequest;

public class AdminLogInDAO {
public AdminBean ab=null; 
public AdminBean login(HttpServletRequest req)
{
	try
	{
		Connection con=DBConnection.getCon();
		PreparedStatement ps=con.prepareStatement("select *from AdminLogIn where username=? and password=?");
		ps.setString(1,req.getParameter("userName"));
		ps.setString(2, req.getParameter("password"));
		ResultSet rs=ps.executeQuery();
		if(rs.next())
		{
			ab=new AdminBean();
			ab.setuName(rs.getString(1));
			ab.setpWord(rs.getString(2));
		}
		
	}
	catch(Exception e)
	{
		e.printStackTrace();
	}
	return ab;
}
}
