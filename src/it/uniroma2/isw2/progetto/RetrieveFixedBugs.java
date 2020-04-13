package it.uniroma2.isw2.progetto;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.io.File;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

	private static final String PROJECT_NAME ="MAHOUT";
	private static final String PROJECT_NAME_GIT ="apache/mahout.git";
	private static final String CLONED_PROJECT_FOLDER = new File("").getAbsolutePath()+"\\"+PROJECT_NAME;	// This give me the localPath of the application where it is installed
	private static final int YEARS_INTERVAL=5; //range degli anni passati su cui cercare
	private static final String CSV_PATH = new File("").getAbsolutePath()+"\\data.csv";
	
    
	private static ArrayList<String> yearsList;
	private static boolean counting=false;
	
	
	
	private static String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}

	public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
		
		InputStream is = new URL(url).openStream();
		try(BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));) {
			String jsonText = readAll(rd);
			JSONObject json = new JSONObject(jsonText);
			return json;
		} finally {
			is.close();
		}
	}

	//questo metodo fa il 'git clone' della repository (necessario per poter ricavare successivamente il log dei commit)   
	private static void gitClone() throws IOException, InterruptedException {

		String originUrl = "https://github.com/"+PROJECT_NAME_GIT;
		
		//percorso dove salvare la directory in locale
		Path directory = Paths.get(CLONED_PROJECT_FOLDER);
		runCommand(directory.getParent(), "git", "clone", originUrl, directory.getFileName().toString());

	}
	//questo metodo fa il comando'git log' sulla repository (mostra il log dei commit)   
	private static void gitLog(String ID) throws IOException, InterruptedException{

		Path directory = Paths.get(CLONED_PROJECT_FOLDER);

		runCommand(directory, "git", "log", "--grep="+ID+":", "-1",
				"--date=short", "--pretty=format:\"%cd\"");


	}
	public static void runCommand(Path directory, String... command) throws IOException, InterruptedException {

		Objects.requireNonNull(directory, "directory Ë NULL");
		//System.out.println(command[2]);
		if (!Files.exists(directory)) {

			throw new RuntimeException("can't run command in non-existing directory '" + directory + "'");

		}

		ProcessBuilder pb = new ProcessBuilder()

				.command(command)

				.directory(directory.toFile());

		//lancio un nuovo processo che invocher‡ il comando command,
		//nella working directory fornita. 
		Process p = pb.start();

		StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), "ERROR");

		StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream(), "OUTPUT");

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

		private StreamGobbler(InputStream is, String type) {

			this.is = is;

		}

		@Override

		public void run() {

			try (BufferedReader br = new BufferedReader(new InputStreamReader(is));) {

				String line;

				while ((line = br.readLine()) != null) {
					if(counting)
						yearsList.add(line.substring(0, 4));
					//System.out.println(line);
				}

			} catch (IOException ioe) {

				ioe.printStackTrace();

			}

		}
	}

	/*
	  Java isn't able to delete folders with data in it. We have to delete
	     all files before deleting the CLONED_PROJECT_FOLDER.This utility class is used to delete 
	  folders recursively in java.*/

	public static void recursiveDelete(File file) {
		//to end the recursive loop
		if (!file.exists())
			return;

		//if directory, go inside and call recursively
		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				//call recursively
				recursiveDelete(f);
			}
		}
		//call delete to delete files and empty directory
		file.delete();
	}


	private static void writeCSV(Map<String,Integer> map) {

		final String[] header = new String[] { "years", "bugs fixed"};

		
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
						
				writer.write(y.toString());
				writer.write(",");
				writer.write(String.valueOf(i));
				writer.write("\r\n");
				}
			writer.close();
				  } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.print("Errore alla scrittura sul file CSV ");
			System.exit(-1);
		}  
		
		

		
		

	}

	public static void main(String[] args) throws IOException, JSONException {

	
		
		
		Integer j = 0, i = 0, total = 1;
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

			//System.out.println(url);

			JSONObject json = readJsonFromUrl(url);
			JSONArray issues = json.getJSONArray("issues");
			//ci si prende il numero totale di ticket recuperati
			total = json.getInt("total");

			ticketIDList= new ArrayList<String>();
			yearsList= new ArrayList<String>();
			// si itera sul numero di ticket
			for (; i < total && i < j; i++) {

				//String data = issues.getJSONObject(i%1000).getJSONObject("fields").get("resolutiondate").toString();
				String key = issues.getJSONObject(i%1000).get("key").toString();

				//System.out.println(/*data+" "+*/key);

				ticketIDList.add(key);

			}  
		} while (i < total);

		//System.out.println(ticketIDList);
		String myID= new String();

		//cancellazione preventiva della directory locale del progetto   
		recursiveDelete(new File(CLONED_PROJECT_FOLDER));

		try {
			gitClone();	
			
			//abilito il salvataggio dei valori dalla riga di output del processo che eseguir‡ il git log
			counting=true;
			for ( i = 0; i < ticketIDList.size(); i++) {
				myID=ticketIDList.get(i);
				gitLog(myID);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.out.print("Errore all'esecuzione del comando git ");
			 Thread.currentThread().interrupt();
			System.exit(-1);
		}
		finally {
			//cancellazione directory locale del progetto   
			recursiveDelete(new File(CLONED_PROJECT_FOLDER));
		}
		Map<String, Integer> map = new HashMap<String,Integer>();

		//popolamento map avente come chiave l'anno e come value il numero di bug risolti
		for(i=0;i<yearsList.size();i++) {
			map.put(yearsList.get(i), (map.getOrDefault(yearsList.get(i), 0)+1));
		}


		//System.out.println(yearsList);
		System.out.println(map);

		writeCSV(map);
  System.out.println("Finito");
		return;
	}




}
