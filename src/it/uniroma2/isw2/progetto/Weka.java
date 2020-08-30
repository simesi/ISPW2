package it.uniroma2.isw2.progetto;
import weka.core.Instances;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.text.DecimalFormat;

import weka.classifiers.Evaluation;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.RandomForest;
import weka.classifiers.bayes.NaiveBayes;
import weka.filters.supervised.instance.Resample;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;
import weka.core.converters.ConverterUtils.DataSource;
import weka.classifiers.evaluation.*;
import weka.classifiers.lazy.IBk;


public class Weka {




	public Weka() {}
	

	//questo metodo compara i risultati dei tre classificatori utilizzando la tecnica WalkForward
	public void doClassification(int Maxversion, String ProjectName) {
		
		String myClassificator=null;
		FileWriter fileWriter=null;
		Evaluation eval = null;
		DecimalFormat numberFormat = new DecimalFormat("0.00");
		
		for(int version=2;version<=Maxversion;version++) {

			String ARFFNAmeFileTrain = "";
			String ARFFNAmeFileTest = "";

			//prima ci si crea un file arff da quello csv
			try {
				// load CSV
				CSVLoader loader = new CSVLoader();
				loader.setSource(new File(ProjectName+" Training for "+"Release "+version+".csv"));
				Instances data = loader.getDataSet();

				// save ARFF
				ArffSaver saver = new ArffSaver();
				saver.setInstances(data);


				ARFFNAmeFileTrain = ProjectName+" Training for "+"Release "+version+".arff";


				saver.setFile(new File(ARFFNAmeFileTrain));
				saver.writeBatch();
				

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(-1);
			}



			//adesso ci si crea l'arff per il testing
			try {
				// load CSV
				CSVLoader loader = new CSVLoader();
				loader.setSource(new File(ProjectName+" Testing for "+"Release "+version+".csv"));
				Instances data = loader.getDataSet();

				// save ARFF
				ArffSaver saver = new ArffSaver();
				saver.setInstances(data);


				ARFFNAmeFileTest = ProjectName +" Testing for "+"Release "+version+".arff";


				saver.setFile(new File(ARFFNAmeFileTest));
				saver.writeBatch();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(-1);
			}


			//ora si classifica ------------------------- 

			try {
				//load datasets
				DataSource source1 = new DataSource(ARFFNAmeFileTrain);
				Instances training = source1.getDataSet();

				DataSource source2 = new DataSource(ARFFNAmeFileTest);
				Instances testing = source2.getDataSet();

				int numAttr = training.numAttributes();
				training.setClassIndex(numAttr - 1); //leviamo 1 perchè l'ultima colonna la vogliamo stimare 
				testing.setClassIndex(numAttr - 1);

				//per ogni classificatore
				for(int n=1;n<=3;n++) {
					if(n==1) {

						//NaiveBayes---------------
						NaiveBayes classifier = new NaiveBayes(); //scelgo come classificatore il naive bayes
						myClassificator ="NaiveBayes";
						classifier.buildClassifier(training); //qui si fa il training

						eval = new Evaluation(testing);	

						eval.evaluateModel(classifier, testing); 
					}

					else if (n==2) {
						//RandomForest---------------
						RandomForest classifier = new RandomForest(); //scelgo come classificatore RandomForest
						myClassificator ="RandomForest";
						classifier.buildClassifier(training); //qui si fa il training

						eval = new Evaluation(testing);	

						eval.evaluateModel(classifier, testing); 
					}
					else if (n==3) {
						//Ibk---------------
						IBk classifier = new IBk(); //scelgo come classificatore Ibk
						myClassificator ="IBk";
						classifier.buildClassifier(training); //qui si fa il training

						eval = new Evaluation(testing);	

						eval.evaluateModel(classifier, testing); 
					}

					//ora si scrive file csv coi risultati

					//se è la prima iterazione
					if( fileWriter==null) {

						String name = ProjectName+" Deliverable 2 Milestone 2.csv";
						
						//True = Append to file, false = Overwrite
						fileWriter = new FileWriter(name,true);
						fileWriter.append("Dataset,#Training Release, Classifier, Precision, Recall, AUC, KAPPA");
						fileWriter.append("\n");
					}
				
					fileWriter.append(ProjectName);
					fileWriter.append(",");
					fileWriter.append(String.valueOf(version-1));
					fileWriter.append(",");
					fileWriter.append(myClassificator);
					fileWriter.append(",");
					fileWriter.append(String.valueOf(numberFormat.format(eval.precision(1))).replace(',', '.'));
					fileWriter.append(",");
					fileWriter.append(String.valueOf(numberFormat.format(eval.recall(1))).replace(',', '.'));
					fileWriter.append(",");
					fileWriter.append(String.valueOf(numberFormat.format(eval.areaUnderROC(1))).replace(',', '.'));
					fileWriter.append(",");
					fileWriter.append(String.valueOf(numberFormat.format(eval.kappa())).replace(',', '.'));
					fileWriter.append("\n");

				}

			}

			catch (Exception e) {
				e.printStackTrace();
				System.exit(-1); ;
				// TODO: handle exception
			}

		}
		    try {
		    	fileWriter.flush();
				fileWriter.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		return;

	}

}



