package test;

public class StudentInfoBean {
	private String regNo,name,branch,email;
	int aYear,pYear;
	float tAmt,rAmt;
	long phNo;
	public StudentInfoBean() {
		super();
	}
	public String getRegNo() {
		return regNo;
	}
	public void setRegNo(String regNo) {
		this.regNo = regNo;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getBranch() {
		return branch;
	}
	public void setBranch(String branch) {
		this.branch = branch;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public int getaYear() {
		return aYear;
	}
	public void setaYear(int aYear) {
		this.aYear = aYear;
	}
	public int getpYear() {
		return pYear;
	}
	public void setpYear(int pYear) {
		this.pYear = pYear;
	}
	public float gettAmt() {
		return tAmt;
	}
	public void settAmt(float tAmt) {
		this.tAmt = tAmt;
	}
	public float getrAmt() {
		return rAmt;
	}
	public void setrAmt(float rAmt) {
		this.rAmt = rAmt;
	}
	public long getPhNo() {
		return phNo;
	}
	public void setPhNo(long phNo) {
		this.phNo = phNo;
	}

}
