package test;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class StudentInfoDAO {

	Connection con;
	public int insert(StudentInfoBean sib)
	{
		int k=0;
	try
	{
		con=DBConnection.getCon();
		PreparedStatement ps=con.prepareStatement("insert into StudentRegistration values(?,?,?,?,?,?,?,?,?)");
		ps.setString(1, sib.getRegNo());
		ps.setString(2, sib.getName());
		ps.setString(3, sib.getBranch());
		ps.setInt(4,sib.getaYear());
		ps.setInt(5,sib.getpYear());
		ps.setFloat(6, sib.gettAmt());
		ps.setFloat(7, sib.getrAmt());
		ps.setString(8, sib.getEmail());
		ps.setLong(9, sib.getPhNo());
		
		 k=ps.executeUpdate();
	}
	catch(Exception e)
	{
		e.printStackTrace();
	}
	return k;	
	}
	
}
