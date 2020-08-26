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

import com.sun.xml.internal.ws.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.io.FileWriter;
import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;

/**
 * Copyright (C) 2020 Simone Mesiano Laureani (a.k.a. Simesi)
 *    
 *    This file is part of the contents developed for the course
 * 	  ISW2 (A.Y. 2019-2020) at Università di Tor Vergata in Rome.
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
public class Main {

	private static String PROJECT_NAME ="MAHOUT";
	private static String PROJECT_NAME_GIT ="apache/mahout.git";
	private static final String CLONED_PROJECT_FOLDER_DELIVERABLE1 = new File("").getAbsolutePath()+"\\"+PROJECT_NAME;	// This give me the localPath of the application where it is installed
	private static final String CSV_PATH = Paths.get(new File("").getAbsolutePath())+"\\Dati Deliverable 1.csv";

	private static final int YEARS_INTERVAL=14; //range degli anni passati su cui cercare (per deliverable 1)
	private static final boolean COLLECT_DATA_AS_YEARS = false;  //impostare come true per impostare come unit� di misura del control chart un anno

	private static ArrayList<String> yearsList;
	private static boolean storeData=false;
	private static boolean startToExecDeliverable2=false;

	//--------------------------
	//per deliverable 2

	public static HashMap<LocalDateTime, String> releaseNames;
	public static HashMap<LocalDateTime, String> releaseID;
	public static ArrayList<LocalDateTime> releases;
	public static HashMap<String,LocalDateTime> fromReleaseIndexToDate=new HashMap<String,LocalDateTime>();
	public static HashMap<String,String> fromFileNameToReleaseIndexOfCreation=new HashMap<String,String>();
	public static HashMap<String,LocalDateTime> fromFileNameToDateOfCreation=new HashMap<String,LocalDateTime>();
	public static Integer numVersions;
	public static ArrayList<String> fileNameOfFirstHalf;
	public static ArrayList<LineOfDataset> arr;

	public static boolean searchingForDateOfCreation = false;
	private static boolean calculatingLOC=false;
	private static boolean calculatingLOC_Touched=false;
	private static boolean calculatingNAuth=false;

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

	//questo metodo fa il comando'git log' del bug sulla repository (mostra il log dei commit)   
	private static void gitLogOfBug(String id) throws IOException, InterruptedException{

		Path directory = Paths.get(CLONED_PROJECT_FOLDER_DELIVERABLE1);

		runCommand(directory, "git", "log", "--grep="+id+":", "-1",
				"--date=short", "--pretty=format:\"%cd\"");


	}
	public static void runCommand(Path directory, String... command) throws IOException, InterruptedException {

		Objects.requireNonNull(directory, "directory � NULL");

		if (!Files.exists(directory)) {

			throw new SecurityException("can't run command in non-existing directory '" + directory + "'");

		}

		ProcessBuilder pb = new ProcessBuilder()

				.command(command)

				.directory(directory.toFile());

		//lancio un nuovo processo che invocher� il comando 'command',
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

		//lancio un nuovo processo che invocher� il comando command,
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
				String filename= "";
				int addedLines=0;
				int deletedLines=0;
				int maxAddedlines=0;
				ArrayList<Integer> addedLinesForEveryRevision;


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

						//levo l'ultimo carattere introdotto (per errore) nel replace a monte
						String file = line.substring(0, line.length()-1);

						LocalDateTime dateTime; 

						String secondLine =br.readLine();
						if (secondLine != null) {
							LocalDate date = LocalDate.parse(secondLine);
							dateTime = date.atStartOfDay();

							fromFileNameToDateOfCreation.put(file,dateTime);
							//le date ulteriori vengono ignorate
							continue;}
					}

					else if (storeData&&startToExecDeliverable2&&calculatingLOC) {

						String nextLine;
						int sumOfRealDeletedLOC=0;
						int realDeletedLOC=0;
						int maxChurn=0;
						int numberOfCommit=0;
						line=line.trim();
						//"one or more whitespaces = \\s+"
						String[] tokens = line.split("\\s+");
						//set a pin for this location
						br.mark(0);

						nextLine =br.readLine();
						//abbiamo raggiunto la fine (l'ultima riga ha il numero di versione)
						if (nextLine == null) {                            //id versione, filename, LOC fino a quella versione
							LineOfDataset l=new LineOfDataset(Integer.parseInt(tokens[0]),filename); //addedLines-deletedLines);
							l.setSize(addedLines-deletedLines);//set del valore di LOC
							l.setNR(numberOfCommit);
							l.setChurn(addedLines-deletedLines -sumOfRealDeletedLOC);
							l.setMax_Churn(maxChurn);
							l.setAVG_Churn(Math.floorDiv(addedLines-deletedLines -sumOfRealDeletedLOC,numberOfCommit));
							arr.add(l);

						}
						else {
							//per NR
							numberOfCommit++;
							//si prende il primo valore (che sar� il numero di linee di codice aggiunte in un commit)
							addedLines=addedLines+Integer.parseInt(tokens[0]);
							//si prende il secondo valore (che sar� il numero di linee di codice rimosse in un commit)
							deletedLines=deletedLines+Integer.parseInt(tokens[1]);
							//per CHURN (togliamo i commit che hanno solo modificato il codice e quindi risultano +1 sia in linee aggiunte che in quelle eliminate)
							if((Integer.parseInt(tokens[0])-Integer.parseInt(tokens[1]))<0){
								realDeletedLOC=Integer.parseInt(tokens[1])-Integer.parseInt(tokens[0]);
								sumOfRealDeletedLOC= sumOfRealDeletedLOC + realDeletedLOC;
							}
							//per MAX_CHURN
							maxChurn=Math.max((Integer.parseInt(tokens[0])-Integer.parseInt(tokens[1])-realDeletedLOC), maxChurn);
							filename=tokens[2];
							br.reset();
						}

					}

					else if (storeData&&startToExecDeliverable2&&calculatingLOC_Touched) {

						addedLinesForEveryRevision=new ArrayList<Integer>();
						String nextLine;
						int total=0;
						int average=0;
						line=line.trim();
						String[] tokens = line.split("\\s+");
						//set a pin for this location
						br.mark(0);

						nextLine =br.readLine();
						//abbiamo raggiunto la fine
						if (nextLine == null) {                            
							//si itera nell'arraylist per cercare l'oggetto giusto da scrivere 
							for (int i = 0; i < arr.size(); i++) {  
								if((arr.get(i).getVersion()==Integer.parseInt(tokens[0]))&& arr.get(i).getFileName()==filename) {
									arr.get(i).setLOC_Touched(addedLines+deletedLines);
									arr.get(i).setMAX_LOC_Added(maxAddedlines);

									//per il AVG_LOC_Added -----------------------
									for(int n=0; n<addedLinesForEveryRevision.size(); n++){
										total = total + addedLinesForEveryRevision.get(n);
									}
									average = Math.floorDiv(addedLinesForEveryRevision.size(),total);
									//--------------------------------------------------
									arr.get(i).setAVG_LOC_Added(average);
									arr.get(i).setLOC_Added(total);
									break;
								}
							}
						}
						else {

							//per il Max_LOC_Added
							maxAddedlines=Math.max(Integer.parseInt(tokens[0]), maxAddedlines);
							//per il AVG_LOC_Added
							addedLinesForEveryRevision.add(Integer.parseInt(tokens[0])-Integer.parseInt(tokens[1]));
							//si prende il primo valore (che sar� il numero di linee di codice aggiunte in un commit)
							addedLines=addedLines+Integer.parseInt(tokens[0]);
							//si prende il secondo valore (che sar� il numero di linee di codice rimosse in un commit)
							deletedLines=deletedLines+Integer.parseInt(tokens[1]);
							filename=tokens[2];
							br.reset();
						}


					}

					else if (storeData&&startToExecDeliverable2&&calculatingNAuth) {

						String nextLine;
						int version;
						line=line.trim();
						int nAuth=0;
						String[] tokens = line.split("\\s+");
						//set a pin for this location
						br.mark(0);

						nextLine =br.readLine();
						//abbiamo raggiunto la fine (l'ultima riga ha il numero di versione)
						if (nextLine == null) { 
							version=Integer.parseInt(tokens[0]);
							filename= tokens[1];
							//cerchiamo l'oggetto giusto su cui scrivere
							for (int i = 0; i < arr.size(); i++) { 
								if((arr.get(i).getVersion()==version) && (arr.get(i).getFileName()==filename)) {
									arr.get(i).setNAuth(nAuth);
								}
							}
						}
						else {
							nAuth++;							
							br.reset();
						}

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

	//questo metodo scrive un file CSV con i dati richiesti per poter creare il control chart
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

	//passando una pathname se ne ricostruisce la data di creazione
	public static void getCreationDate(String filename) {

		//directory da cui far partire il comando git    
		Path directory = Paths.get(new File("").getAbsolutePath()+"\\"+PROJECT_NAME);

		//chiamata per ottenere la data di creazione del file e inserirla in una hashMap
		try {
			String command = "echo "+filename+" && git log --diff-filter=A --format=%as --reverse -- "
					+filename;
			//System.out.println(command);
			runCommandOnShell(directory, command);

		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(-1);
		}



		//per ogni versione della prima met� delle release
		for (int i = 1; i <= Math.floorDiv(fromReleaseIndexToDate.size(),2); i++) {

			if ((fromFileNameToDateOfCreation.get(filename).isAfter(fromReleaseIndexToDate.get(String.valueOf(i)))||
					fromFileNameToDateOfCreation.get(filename).isEqual(fromReleaseIndexToDate.get(String.valueOf(i))))&&
					fromFileNameToDateOfCreation.get(filename).isBefore(fromReleaseIndexToDate.get(String.valueOf(i+1)))) {
				fromFileNameToReleaseIndexOfCreation.put(filename,String.valueOf(i));
				fileNameOfFirstHalf.add(filename);
				break;

			}

		}

	}


	//Search and list of all file java in the repository (at this time)
	public static void searchFileJava( final File folder, List<String> result) {
		String fileRenamed;
		for (final File f : folder.listFiles()) {

			if (f.isDirectory()) {
				searchFileJava(f, result);
			}

			if (f.isFile()) {
				//si prendono solo i file java
				if (f.getName().matches(".*\\.java")) {

					fileRenamed=f.getAbsolutePath();
					//discard of the local prefix to the file name (that depends to this program)

					fileRenamed=fileRenamed.replace((Paths.get(new File("").getAbsolutePath()+"\\"+PROJECT_NAME)+"\\").toString(), "");
					//System.out.println(s);
					//ci si costruisce una HashMap con la data di creazione dei file java

					//il comando git log prende percorsi con la '/'
					fileRenamed= fileRenamed.replace("\\", "/");
					result.add(fileRenamed);
				}
			}
		}
	}


	//data una versione/release e un filename si ricava il LOC/size del file
	private static void getChurnMetrics(String filename, Integer i) {



		//directory da cui far partire il comando git    
		Path directory = Paths.get(new File("").getAbsolutePath()+"\\"+PROJECT_NAME);
		String command;

		try {

			command = "git log --until="+fromReleaseIndexToDate.get(String.valueOf(i))	+" --format= --numstat -- "+filename+" && echo "+i;	

			runCommandOnShell(directory, command);

		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(-1);
		}

	}


	private static void getLOCMetrics(String filename, Integer i) {
		//directory da cui far partire il comando git    
		Path directory = Paths.get(new File("").getAbsolutePath()+"\\"+PROJECT_NAME);
		String command;

		try {
			if(i>1) {
				command = "git log --since="+fromReleaseIndexToDate.get(String.valueOf(i-1))+" --until="+fromReleaseIndexToDate.get(String.valueOf(i))	+" --format= --numstat -- "+filename+" && echo "+i;	
			}
			else {  //prima release
				command = "git log --until="+fromReleaseIndexToDate.get(String.valueOf(i))	+" --format= --numstat -- "+filename+" && echo "+i;	
			}
			runCommandOnShell(directory, command);

		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(-1);
		}


	}

	private static void getNumberOfAuthors(String filename, Integer i) {

		//directory da cui far partire il comando git    
		Path directory = Paths.get(new File("").getAbsolutePath()+"\\"+PROJECT_NAME);
		String command;

		try {

			command = "git shortlog -sn --all --until="+fromReleaseIndexToDate.get(String.valueOf(i))	+" "+filename+" && echo "+i+" "+filename;	

			runCommandOnShell(directory, command);

		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(-1);
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

		// INIZIO DELIVERABLE 1
		//cancellazione preventiva della directory clonata del progetto (se gi� esistente)   
		recursiveDelete(new File(CLONED_PROJECT_FOLDER_DELIVERABLE1));
		try {
			gitClone();	

			//abilito il salvataggio dei valori ottenuti dalla riga di output del processo che eseguir� il git log
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

		//aggiunta dei mesi con valori nulli
		if(!COLLECT_DATA_AS_YEARS) {
			// TreeMap to store values of HashMap 
			TreeMap<String, Integer> sorted = new TreeMap<>(); 
			// Copy all data from hashMap into TreeMap 
			sorted.putAll(map); 

			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
			DateTimeFormatter formatterWithNoDay = DateTimeFormatter.ofPattern("yyyy-MM");

			//si prende il primo e l'ultimo anno-mese ....
			LocalDate firstdate = LocalDate.parse(sorted.firstKey()+"-01",formatter);
			LocalDate lastdate = LocalDate.parse(sorted.lastKey()+"-01",formatter);
			//iteratore
			LocalDate date = firstdate;

			// .... e si aggiungono i mesi tra i due periodi 			
			while(date.isBefore(lastdate)) {
				date =date.with(TemporalAdjusters.firstDayOfNextMonth());
				sorted.put(date.format(formatterWithNoDay), 0);
			}

			//con l'istruzione seguente i valori dele chiavi duplicate in 'sorted' verranno riscritte con i valori di 'map'.
			sorted.putAll(map);
			map=sorted;
		}


		writeCSV(map);
		System.out.println("Finito deliverable 1");

		/*FINE DELIVERABLE 1*/

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

		//--------------------------------------------------------
		///ORA CREO IL  DATASET


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
		List<String> files = new ArrayList<>();
		fileNameOfFirstHalf = new ArrayList<String>();

		//search for java files in the cloned repository
		searchFileJava(folder, files);
		//System.out.println(result.get(702));
		//	System.out.println(result.get(703));

		//popolo un'HasMap con associazione indice di release-data delle release
		for ( i = 1; i <= releases.size(); i++) {
			fromReleaseIndexToDate.put(i.toString(),releases.get(i-1));
		}

		storeData=true;
		searchingForDateOfCreation = true;
		//per ogni file
		for (String s : files) {


			getCreationDate(s);		
			//System.out.println(s+" "+fromFileNameToReleaseIndexOfCreation.get(s));
		}
		searchingForDateOfCreation = false;


		//System.out.println(fromFileNameToReleaseIndexOfCreation.size());

		//----------------------------------------------
		//System.out.println(fileNameOfFirstHalf);


		int num=0;
		calculatingLOC = true;
		//per ogni indice di versione nella prim� met� delle release
		for(i=1;i<=Math.floorDiv(fromReleaseIndexToDate.size(),2);i++) {
			//per ogni file
			for (String s : fileNameOfFirstHalf) {
				num++;
				//il metodo getLOC creer� l'arrayList di entry LineOfDataSet
				getChurnMetrics(s,i);
				calculatingLOC = false;
				calculatingLOC_Touched = true;
				//i metodi successivi modificano semplicemente le entry in quell'array
				getLOCMetrics(s,i);
				calculatingLOC_Touched = false;
				calculatingNAuth= true;
				getNumberOfAuthors(s,i);




				//LineOfDataset line = new LineOfDataset(1, fromFileNameToReleaseIndexOfCreation, fileName, size, lOC_Touched, nR, nFix, nAuth, lOC_Added, mAX_LOC_Added, aVG_LOC_Added, age, buggy)
			}

		} 
		calculatingLOC = false;




		/*----------------------------
		 //parte per OpenJPA

		fileWriter = null;
		try {
			fileWriter = null;
			String outname = PROJECT_NAME + "Deliverable2.csv";
			//Name of CSV for output
			fileWriter = new FileWriter(outname);
			fileWriter.append("Version,File Name,Size(LOC), LOC_Touched,NR,NFix,NAuth,LOC_Added,MAX_LOC_Added,AVG_LOC_Added,Age,Buggy");
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