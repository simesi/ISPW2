package it.uniroma2.isw2.progetto;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.io.File;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import sun.text.resources.cldr.fil.FormatData_fil;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.io.FileWriter;

/**
 * Copyright (C) 2020 Simone Mesiano Laureani (a.k.a. Simesi)
 *    
 *    This file is part of the contents developed for the course
 * 	  ISW2 (A.Y. 2019-2020) at Universit√† di Tor Vergata in Rome.
 *
 *    This is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as 
 *    published by the Free Software Foundation, either version 3 of the 
 *    License, or (at your option) any later version.
 *
 *    This software is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this source.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * @author simone
 *
 */
public class RetrieveFixedBugs {

	private static String PROJECT_NAME ="MAHOUT";
	private static String PROJECT_NAME_GIT ="apache/mahout.git";
	private static final String CLONED_PROJECT_FOLDER_DELIVERABLE1 = new File("").getAbsolutePath()+"\\"+PROJECT_NAME;	// This give me the localPath of the application where it is installed
	private static final String CSV_PATH = Paths.get(new File("").getAbsolutePath())+"\\data.csv";

	private static final int YEARS_INTERVAL=14; //range degli anni passati su cui cercare
	private static final boolean COLLECT_DATA_AS_YEARS = false; 

	private static ArrayList<String> yearsList;
	private static boolean storeData=false;
	private static boolean startToExecDeliverable2=false;

	//--------------------------
	//per deliverable 2

	public static HashMap<LocalDateTime, String> releaseNames;
	public static HashMap<LocalDateTime, String> releaseID;
	public static ArrayList<LocalDateTime> releases;
	public static HashMap<String,LocalDateTime> fromIndexToDate=new HashMap<String,LocalDateTime>();
	public static HashMap<String,String> fromFileNameToIndexOfCreation=new HashMap<String,String>();
	public static HashMap<String,LocalDateTime> fromFileNameToDateOfCreation=new HashMap<String,LocalDateTime>();
	public static Integer numVersions;
	
	public static boolean searchingForDateOfCreation = false;
	
	//--------------------------


	private static String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}

	public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
		JSONObject json;
		InputStream is = new URL(url).openStream();
		try(BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
			String jsonText = readAll(rd);
			json = new JSONObject(jsonText);

		} finally {
			is.close();
		}
		return json;
	}

	//questo metodo fa il 'git clone' della repository (necessario per poter ricavare successivamente il log dei commit)   
	private static void gitClone() throws IOException, InterruptedException {

		Path directory;
		String originUrl = "https://github.com/"+PROJECT_NAME_GIT;

		if (startToExecDeliverable2==false) {
			//percorso dove salvare la directory in locale
			directory = Paths.get(CLONED_PROJECT_FOLDER_DELIVERABLE1);
		}
		else {
			directory = Paths.get(new File("").getAbsolutePath()+"\\"+PROJECT_NAME);
		}
		runCommand(directory.getParent(), "git", "clone", originUrl, directory.getFileName().toString());

	}
	//questo metodo fa il comando'git log' sulla repository (mostra il log dei commit)   
	private static void gitLogOfBug(String id) throws IOException, InterruptedException{

		Path directory = Paths.get(CLONED_PROJECT_FOLDER_DELIVERABLE1);

		runCommand(directory, "git", "log", "--grep="+id+":", "-1",
				"--date=short", "--pretty=format:\"%cd\"");


	}
	public static void runCommand(Path directory, String... command) throws IOException, InterruptedException {

		Objects.requireNonNull(directory, "directory Ë NULL");

		if (!Files.exists(directory)) {

			throw new SecurityException("can't run command in non-existing directory '" + directory + "'");

		}
		
		ProcessBuilder pb = new ProcessBuilder()

				.command(command)

				.directory(directory.toFile());

		//lancio un nuovo processo che invocher‡ il comando command,
		//nella working directory fornita. 
		Process p = pb.start();

		StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream());

		StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream());

		outputGobbler.start();

		errorGobbler.start();
			
		int exit = p.waitFor();

		errorGobbler.join();

		outputGobbler.join();

		if (exit != 0) {

			throw new AssertionError(String.format("runCommand returned %d", exit));

		}

	}
	
	public static void runCommandOnShell(Path directory, String command) throws IOException, InterruptedException {
		
		//ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c","dir && echo hello");
		ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c","E: && cd "+directory.toString()+" && "+command);	

		//lancio un nuovo processo che invocher‡ il comando command,
		//nella working directory fornita. 
		Process p = pb.start();

		StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream());

		StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream());

		outputGobbler.start();

		errorGobbler.start();
			
		int exit = p.waitFor();

		errorGobbler.join();

		outputGobbler.join();

		if (exit != 0) {

			throw new AssertionError(String.format("runCommand returned %d", exit));

		}

	}
	
	
	

	private static class StreamGobbler extends Thread {

		private final InputStream is;

		private StreamGobbler(InputStream is) {

			this.is = is;

		}

		@Override

		public void run() {

			try (BufferedReader br = new BufferedReader(new InputStreamReader(is));) {

				String line;

				while ((line = br.readLine()) != null) {
					if(storeData&&(!startToExecDeliverable2)) {
						if(COLLECT_DATA_AS_YEARS) {
							//get only year
							yearsList.add(line.substring(0, 4));
						} else {
							//get month and year
							yearsList.add(line.substring(0, 7));
						}
					}
					else if (storeData&&startToExecDeliverable2&&searchingForDateOfCreation) {
						
						//levo l'ultimo carattere introdotto per errore nel replace a monte
						String file = line.substring(0, line.length()-1);
						
						LocalDateTime dateTime; 
						
						String secondLine =br.readLine();
						LocalDate date = LocalDate.parse(secondLine);
						 dateTime = date.atStartOfDay();
						 
					fromFileNameToDateOfCreation.put(file,dateTime);
						
						continue;
					}
					System.out.println("Linea fuori if: "+line);
				}

			} catch (IOException ioe) {

				ioe.printStackTrace();

			}

		}
	}

	/*
	  Java isn't able to delete folders with data in it. We have to delete
	     all files before deleting the CLONED_PROJECT_FOLDER_DELIVERABLE1.This utility class is used to delete 
	  folders recursively in java.*/

	public static void recursiveDelete(File file) {
		//to end the recursive loop
		if (!file.exists())
			return;

		//if directory exists, go inside and call recursively
		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				//call recursively
				recursiveDelete(f);
			}
		}
		//call delete to delete files and empty directory
		try {
			//disabling Read Only property of the file to be deleted resolves the issue triggered by Files.delete
			Files.setAttribute(file.toPath(), "dos:readonly", false);
			Files.deleteIfExists(file.toPath());
		} catch (IOException| InvalidPathException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}


	private static void writeCSV(Map<String,Integer> map) {
		String[] header;
		if(COLLECT_DATA_AS_YEARS) {
			header = new String[] { "years", "bugs fixed"};
		} else {
			header = new String[] { "years-month", "bugs fixed"};
		}

		try (FileWriter writer = new FileWriter(CSV_PATH, false)){
			//True = Append to file, false = Overwrite

			writer.write(header[0]);
			writer.write(",");
			writer.write(header[1]); 
			writer.write("\r\n");

			// Get each keys and values and write on file
			for (Map.Entry<String, Integer> entry : map.entrySet()) {
				String y = entry.getKey();
				Integer i = entry.getValue();

				writer.write(y);
				writer.write(",");
				writer.write(String.valueOf(i));
				writer.write("\r\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}  

	}

	//------------------------------------------
	//Metodi per Deliverable 2 Milestone 1

	public static void addRelease(String strDate, String name, String id) {
		LocalDate date = LocalDate.parse(strDate);
		LocalDateTime dateTime = date.atStartOfDay();
		if (!releases.contains(dateTime))
			releases.add(dateTime);
		releaseNames.put(dateTime, name);
		releaseID.put(dateTime, id);
		return;
	}

	public static void getCreationDate(String filename) {

		searchingForDateOfCreation = true;
		
		//directory da cui far partire il comando git    
		Path directory = Paths.get(new File("").getAbsolutePath()+"\\"+PROJECT_NAME);
		 				 	 
		 
		//file da analizzare
		String fileReformatted = filename;
		//il comando git log prende percorsi con la '/'
		fileReformatted= fileReformatted.replace("\\", "/");
		
		
		//chiamata per ottenere la data di creazione del file e inserirla in una hashMap
		try {
			String command = "echo "+fileReformatted+" && git log --diff-filter=A --format=%as --reverse -- "
		+fileReformatted;
			//System.out.println(command);
			runCommandOnShell(directory, command);
		
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		
		
		//per ogni versione
		for (int i = 1; i <= fromIndexToDate.size(); i++) {
		
		
			
			
		if (fromFileNameToDateOfCreation.get(fileReformatted).isAfter(fromIndexToDate.get(String.valueOf(i)))||
				fromFileNameToDateOfCreation.get(fileReformatted).isEqual(fromIndexToDate.get(String.valueOf(i)))) {
			fromFileNameToIndexOfCreation.put(fileReformatted,String.valueOf(i));
			
			break;
			
		}
		}
		searchingForDateOfCreation = false;
	}


	//Search and list of all file java in the repository
	public static void searchFileJava( final File folder, List<String> result) {
		for (final File f : folder.listFiles()) {

			if (f.isDirectory()) {
				searchFileJava(f, result);
			}

			if (f.isFile()) {
				if (f.getName().matches(".*\\.java")) {
					result.add(f.getAbsolutePath());
				}
			}
		}
	}




	//--------------------------------------

	public static void main(String[] args) throws IOException, JSONException {

		Integer j = 0;
		Integer i = 0;
		Integer total = 1;
		ArrayList<String> ticketIDList;
		//Get JSON API for closed bugs w/ AV in the project
		do {
			//Only gets a max of 1000 at a time, so must do this multiple times if bugs >1000
			j = i + 1000;

			/*Si ricavano tutti i ticket di tipo bug nello stato di risolto o chiuso e con risoluzione "fixed".*/
			String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
					+ PROJECT_NAME + "%22AND%22issueType%22=%22Bug%22AND(%22status%22=%22closed%22OR"
					+ "%22status%22=%22resolved%22)AND%22resolution%22=%22fixed%22AND%20updated%20%20%3E%20endOfYear(-"+YEARS_INTERVAL+")"
					+ "&fields=key,resolutiondate,created&startAt="
					+ i.toString() + "&maxResults=" + j.toString();


			JSONObject json = readJsonFromUrl(url);
			JSONArray issues = json.getJSONArray("issues");
			//ci si prende il numero totale di ticket recuperati
			total = json.getInt("total");

			ticketIDList= new ArrayList<>();
			yearsList= new ArrayList<>();
			// si itera sul numero di ticket
			for (; i < total && i < j; i++) {

				String key = issues.getJSONObject(i%1000).get("key").toString();


				ticketIDList.add(key);

			}  
		} while (i < total);


		String myID;
		/*
		//cancellazione preventiva della directory clonata del progetto (se esiste)   
		recursiveDelete(new File(CLONED_PROJECT_FOLDER_DELIVERABLE1));
		try {
			gitClone();	

			//abilito il salvataggio dei valori dalla riga di output del processo che eseguir‡ il git log
			storeData=true;
			for ( i = 0; i < ticketIDList.size(); i++) {
				myID=ticketIDList.get(i);
				gitLogOfBug(myID);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			Thread.currentThread().interrupt();
			System.exit(-1);
		}
		finally {
			//cancellazione directory clonata locale del progetto   
			recursiveDelete(new File(CLONED_PROJECT_FOLDER_DELIVERABLE1));
		}
		Map<String, Integer> map = new HashMap<>();


		//popolamento map avente come chiave l'anno (e il mese se impostato COLLECT_DATA_AS_YEARS= false) e come value il numero di bug risolti
		for(i=0;i<yearsList.size();i++) {
			map.put(yearsList.get(i), (map.getOrDefault(yearsList.get(i), 0)+1));
		}
		writeCSV(map);
		System.out.println("Finito deliverable 1");
		 */

		//-------------------------------------------------------------------------------------------------
		//INIZIO MILESTONE 1 DELIVERABLE 2 PROJECT 'BOOKKEEPER'

		PROJECT_NAME ="BOOKKEEPER";
		PROJECT_NAME_GIT ="apache/bookkeeper.git";
		startToExecDeliverable2=true;
		storeData=false;
		
		//Fills the arraylist with releases dates and orders them
		//Ignores releases with missing dates
		releases = new ArrayList<LocalDateTime>();
		i=0;
		String url = "https://issues.apache.org/jira/rest/api/2/project/" + PROJECT_NAME;
		JSONObject json = readJsonFromUrl(url);
		JSONArray versions = json.getJSONArray("versions");
		releaseNames = new HashMap<LocalDateTime, String>();
		releaseID = new HashMap<LocalDateTime, String> ();
		for (i = 0; i < versions.length(); i++ ) {
			String name = "";
			String id = "";
			if(versions.getJSONObject(i).has("releaseDate")) {
				if (versions.getJSONObject(i).has("name"))
					name = versions.getJSONObject(i).get("name").toString();
				if (versions.getJSONObject(i).has("id"))
					id = versions.getJSONObject(i).get("id").toString();
				addRelease(versions.getJSONObject(i).get("releaseDate").toString(),
						name,id);
			}
		}
		// order releases by date
		Collections.sort(releases, new Comparator<LocalDateTime>(){
			//@Override
			public int compare(LocalDateTime o1, LocalDateTime o2) {
				return o1.compareTo(o2);
			}
		});
		if (releases.size() < 6)
			return;
		FileWriter fileWriter = null;
		try {
			fileWriter = null;
			String outname = PROJECT_NAME + "VersionInfo.csv";
			//Name of CSV for output
			fileWriter = new FileWriter(outname);
			fileWriter.append("Index,Version ID,Version Name,Date");
			fileWriter.append("\n");
			
			numVersions = releases.size();
			for ( i = 0; i < releases.size(); i++) {
				Integer index = i + 1;
				fileWriter.append(index.toString());
				fileWriter.append(",");
				fileWriter.append(releaseID.get(releases.get(i)));
				fileWriter.append(",");
				fileWriter.append(releaseNames.get(releases.get(i)));
				fileWriter.append(",");
				fileWriter.append(releases.get(i).toString());
				fileWriter.append("\n");
			}

		} catch (Exception e) {
			System.out.println("Error in csv writer");
			e.printStackTrace();
		} finally {
			try {
				fileWriter.flush();
				fileWriter.close();
			} catch (IOException e) {
				System.out.println("Error while flushing/closing fileWriter !!!");
				e.printStackTrace();
			}
		}
		//--------------------------------------------------------
		///ORA CREO IL VERO DATASET
		
		
		//cancellazione preventiva della directory clonata del progetto (se esiste)   
		recursiveDelete(new File(new File("").getAbsolutePath()+"\\"+PROJECT_NAME));
		try {
			gitClone();	
		}
		catch (InterruptedException e) {
			e.printStackTrace();
			Thread.currentThread().interrupt();
			System.exit(-1);
		}


		File folder = new File(PROJECT_NAME);
		List<String> result = new ArrayList<>();

		//search for java files in the cloned rep
		searchFileJava(folder, result);
		//System.out.println(result.get(702));
	//	System.out.println(result.get(703));
		//popolo un'HasMap con associazione index-data
		for ( i = 1; i <= releases.size(); i++) {
			fromIndexToDate.put(i.toString(),releases.get(i-1));
		}
		
		storeData=true;
		
       //per ogni file
		for (String s : result) {
			
			//discard of the local prefix to the file name 
			s=s.replace((Paths.get(new File("").getAbsolutePath()+"\\"+PROJECT_NAME)+"\\").toString(), "");
			//System.out.println(s);
			//ci si costruisce una HashMap con la data di creazione dei file java
			getCreationDate(s);		
		}
System.out.println(fromFileNameToIndexOfCreation);
		
		/*----------------------------
		fileWriter = null;
		try {
			fileWriter = null;
			String outname = PROJECT_NAME + "Deliverable2.csv";
			//Name of CSV for output
			fileWriter = new FileWriter(outname);
			fileWriter.append("Version,File Name,Size(LOC), LOC_Touched,NR,NFix,NAuth,LOC_Added,MAX LOC Added,AVG_LOC_Added,Age,Buggy");
			fileWriter.append("\n");
			numVersions = releases.size();
			for ( i = 0; i < releases.size(); i++) {
				Integer index = i + 1;
				fileWriter.append(index.toString());
				fileWriter.append(",");
				fileWriter.append(releaseID.get(releases.get(i)));
				fileWriter.append(",");
				fileWriter.append(releaseNames.get(releases.get(i)));
				fileWriter.append(",");
				fileWriter.append(releases.get(i).toString());
				fileWriter.append("\n");
			}

		} catch (Exception e) {
			System.out.println("Error in csv writer");
			e.printStackTrace();
		} finally {
			try {
				fileWriter.flush();
				fileWriter.close();
			} catch (IOException e) {
				System.out.println("Error while flushing/closing fileWriter !!!");
				e.printStackTrace();
			}
		}

		 *////////

		//cancellazione directory clonata locale del progetto   
		recursiveDelete(new File(new File("").getAbsolutePath()+"\\"+PROJECT_NAME));

		//------------------------------------------------------------------------------------------------------

		//Deliverable 2, Milestone 1, ProjeCT 'OpenJPA'
		/*------------------
		PROJECT_NAME ="OPENJPA";
		//Fills the arraylist with releases dates and orders them
		//Ignores releases with missing dates
		releases = new ArrayList<LocalDateTime>();
		i=0;
		url = "https://issues.apache.org/jira/rest/api/2/project/" + PROJECT_NAME;
		json = readJsonFromUrl(url);
		versions = json.getJSONArray("versions");
		releaseNames = new HashMap<LocalDateTime, String>();
		releaseID = new HashMap<LocalDateTime, String> ();
		for (i = 0; i < versions.length(); i++ ) {
			String name = "";
			String id = "";
			if(versions.getJSONObject(i).has("releaseDate")) {
				if (versions.getJSONObject(i).has("name"))
					name = versions.getJSONObject(i).get("name").toString();
				if (versions.getJSONObject(i).has("id"))
					id = versions.getJSONObject(i).get("id").toString();
				addRelease(versions.getJSONObject(i).get("releaseDate").toString(),
						name,id);
			}
		}
		// order releases by date
		Collections.sort(releases, new Comparator<LocalDateTime>(){
			//@Override
			public int compare(LocalDateTime o1, LocalDateTime o2) {
				return o1.compareTo(o2);
			}
		});
		if (releases.size() < 6)
			return;
		fileWriter = null;
		try {
			fileWriter = null;
			String outname = PROJECT_NAME + "VersionInfo.csv";
			//Name of CSV for output
			fileWriter = new FileWriter(outname);
			fileWriter.append("Index,Version ID,Version Name,Date");
			fileWriter.append("\n");
			numVersions = releases.size();
			for ( i = 0; i < releases.size(); i++) {
				Integer index = i + 1;
				fileWriter.append(index.toString());
				fileWriter.append(",");
				fileWriter.append(releaseID.get(releases.get(i)));
				fileWriter.append(",");
				fileWriter.append(releaseNames.get(releases.get(i)));
				fileWriter.append(",");
				fileWriter.append(releases.get(i).toString());
				fileWriter.append("\n");
			}

		} catch (Exception e) {
			System.out.println("Error in csv writer");
			e.printStackTrace();
		} finally {
			try {
				fileWriter.flush();
				fileWriter.close();
			} catch (IOException e) {
				System.out.println("Error while flushing/closing fileWriter !!!");
				e.printStackTrace();
			}
		}
		 ************************/
		return;
	}




}
