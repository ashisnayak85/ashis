package test;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.http.HttpServletRequest;
public class StudentLogInDAO {
	public StudentInfoBean sb=null; 
	public StudentInfoBean login(HttpServletRequest req)
	{
		try
		{
			Connection con=DBConnection.getCon();
			PreparedStatement ps=con.prepareStatement("select *from StudentRegistration where regNo=? and =phNumber?");
			ps.setString(1,req.getParameter("userName"));
			ps.setString(2, req.getParameter("password"));
			ResultSet rs=ps.executeQuery();
			if(rs.next())
			{
				sb=new StudentInfoBean();
				sb.setRegNo(rs.getString(1));
				sb.setPhNo(rs.getLong(2));
			}
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return sb;
	}
}
