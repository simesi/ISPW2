package it.uniroma2.isw2.progetto;

public class TicketTakenFromJIRA {
     private String key;
     private String createdVersion;
     private String AffectedVersion;
     private String FixedVersion;
     
     
     
	public TicketTakenFromJIRA(String key, String createdVersion, String affectedVersion) {
		super();
		this.key = key;
		this.createdVersion = createdVersion;
		AffectedVersion = affectedVersion;
	}




	/**
	 * @return the key
	 */
	public String getKey() {
		return key;
	}



	/**
	 * @param key the key to set
	 */
	public void setKey(String key) {
		this.key = key;
	}



	/**
	 * @return the createdVersion
	 */
	public String getCreatedVersion() {
		return createdVersion;
	}



	/**
	 * @param createdVersion the createdVersion to set
	 */
	public void setCreatedVersion(String createdVersion) {
		this.createdVersion = createdVersion;
	}



	/**
	 * @return the affectedVersion
	 */
	public String getAffectedVersion() {
		return AffectedVersion;
	}



	/**
	 * @param affectedVersion the affectedVersion to set
	 */
	public void setAffectedVersion(String affectedVersion) {
		AffectedVersion = affectedVersion;
	}



	/**
	 * @return the fixedVersion
	 */
	public String getFixedVersion() {
		return FixedVersion;
	}



	/**
	 * @param fixedVersion the fixedVersion to set
	 */
	public void setFixedVersion(String fixedVersion) {
		FixedVersion = fixedVersion;
	}

}
