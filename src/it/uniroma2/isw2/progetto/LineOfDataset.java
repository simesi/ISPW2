package it.uniroma2.isw2.progetto;

public class LineOfDataset {

	private int numeroDiRiga;
	private int version;//id versione
	private String fileName;
	private int Size;
	private int Max_Churn;
	private int AVG_Churn;
	private int LOC_Touched;
	private int NR;
	private int Churn;
	private int NAuth;
	private int LOC_Added;
	private int MAX_LOC_Added;
	private int AVG_LOC_Added;
	private int Age;
	private String Buggy;

	
	/**
	 * @return the version
	 */
	public int getVersion() {
		return version;
	}


	/**
	 * @param version the version to set
	 */
	public void setVersion(int version) {
		this.version = version;
	}


	/**
	 * @return the fileName
	 */
	public String getFileName() {
		return fileName;
	}


	/**
	 * @param fileName the fileName to set
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * @return the max_Churn
	 */
	public int getMax_Churn() {
		return Max_Churn;
	}


	/**
	 * @param max_Churn the max_Churn to set
	 */
	public void setMax_Churn(int max_Churn) {
		Max_Churn = max_Churn;
	}


	/**
	 * @return the aVG_Churn
	 */
	public int getAVG_Churn() {
		return AVG_Churn;
	}


	/**
	 * @param aVG_Churn the aVG_Churn to set
	 */
	public void setAVG_Churn(int aVG_Churn) {
		AVG_Churn = aVG_Churn;
	}
	
	/**
	 * @return the size
	 */
	public int getSize() {
		return Size;
	}


	/**
	 * @param size the size to set
	 */
	public void setSize(int size) {
		Size = size;
	}


	/**
	 * @return the lOC_Touched
	 */
	public int getLOC_Touched() {
		return LOC_Touched;
	}


	/**
	 * @param lOC_Touched the lOC_Touched to set
	 */
	public void setLOC_Touched(int lOC_Touched) {
		LOC_Touched = lOC_Touched;
	}


	/**
	 * @return the nR
	 */
	public int getNR() {
		return NR;
	}


	/**
	 * @param nR the nR to set
	 */
	public void setNR(int nR) {
		NR = nR;
	}


	/**
	 * @return the Churn
	 */
	public int getChurn() {
		return Churn;
	}


	/**
	 * @param Churn the Churn to set
	 */
	public void setChurn(int churn) {
		Churn = churn;
	}


	/**
	 * @return the nAuth
	 */
	public int getNAuth() {
		return NAuth;
	}


	/**
	 * @param nAuth the nAuth to set
	 */
	public void setNAuth(int nAuth) {
		NAuth = nAuth;
	}


	/**
	 * @return the lOC_Added
	 */
	public int getLOC_Added() {
		return LOC_Added;
	}


	/**
	 * @param lOC_Added the lOC_Added to set
	 */
	public void setLOC_Added(int lOC_Added) {
		LOC_Added = lOC_Added;
	}


	/**
	 * @return the mAX_LOC_Added
	 */
	public int getMAX_LOC_Added() {
		return MAX_LOC_Added;
	}


	/**
	 * @param mAX_LOC_Added the mAX_LOC_Added to set
	 */
	public void setMAX_LOC_Added(int mAX_LOC_Added) {
		MAX_LOC_Added = mAX_LOC_Added;
	}


	/**
	 * @return the aVG_LOC_Added
	 */
	public int getAVG_LOC_Added() {
		return AVG_LOC_Added;
	}


	/**
	 * @param aVG_LOC_Added the aVG_LOC_Added to set
	 */
	public void setAVG_LOC_Added(int aVG_LOC_Added) {
		AVG_LOC_Added = aVG_LOC_Added;
	}


	/**
	 * @return the age
	 */
	public int getAge() {
		return Age;
	}


	/**
	 * @param age the age to set
	 */
	public void setAge(int age) {
		Age = age;
	}


	/**
	 * @return the buggy
	 */
	public String getBuggy() {
		return Buggy;
	}


	/**
	 * @param buggy the buggy to set
	 */
	public void setBuggy(String buggy) {
		Buggy = buggy;
	}


	public LineOfDataset(int numeroDiRiga, int version, String fileName, int size, int lOC_Touched, int nR, int churn,
			int nAuth, int lOC_Added, int mAX_LOC_Added, int aVG_LOC_Added, int age, String buggy) {
		super();
		this.numeroDiRiga = numeroDiRiga;
		this.version = version;
		this.fileName = fileName;
		Size = size;
		LOC_Touched = lOC_Touched;
		NR = nR;
		Churn = churn;
		NAuth = nAuth;
		LOC_Added = lOC_Added;
		MAX_LOC_Added = mAX_LOC_Added;
		AVG_LOC_Added = aVG_LOC_Added;
		Age = age;
		Buggy = buggy;
	}

             //costruttore minimale                                             
	public LineOfDataset(int version, String fileName) {
		super();
		this.version = version;
		this.fileName = fileName;
		
	}

	

}
