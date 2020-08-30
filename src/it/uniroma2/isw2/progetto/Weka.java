package it.uniroma2.isw2.progetto;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.text.DecimalFormat;

import weka.attributeSelection.CfsSubsetEval;
import weka.attributeSelection.GreedyStepwise;
import weka.classifiers.Evaluation;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.RandomForest;
import weka.classifiers.bayes.NaiveBayes;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SMOTE;
import weka.filters.supervised.instance.SpreadSubsample;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;
import weka.core.converters.ConverterUtils.DataSource;
import weka.classifiers.evaluation.*;
import weka.classifiers.lazy.IBk;


public class Weka {




	public Weka() {}


	//questo metodo compara i risultati dei tre classificatori utilizzando la tecnica WalkForward
	public void doClassificationMilestone2(int Maxversion, String ProjectName) {

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
				fileWriter.close();
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


	public void doClassificationMilestone3(int Maxversion, String ProjectName) {


		String myClassificator=null;
		FileWriter fileWriter=null;
		Evaluation eval = null;
		DecimalFormat numberFormat = new DecimalFormat("0.00");
		int numAttrFiltered=0;
		int numAttrNoFilter=0;
		int numDefectiveTrain=0;
		int numDefectiveTest=0;
		int percentInstOfMajorityClass=0;
		Resample resample= null;

		try {
			for(int version=2;version<=Maxversion;version++) {


				DataSource source = new DataSource(ProjectName +" Training for "+"Release "+version+".arff");

				DataSource source2 = new DataSource(ProjectName +" Testing for "+"Release "+version+".arff");

				Instances noFilterTraining = source.getDataSet();
				Instances testing = source2.getDataSet();
				Instances filteredTraining = null;
				Instances testingFiltered = null;

				//stima senza filtri
				numAttrNoFilter = noFilterTraining.numAttributes();
				noFilterTraining.setClassIndex(numAttrNoFilter - 1);
				testing.setClassIndex(numAttrNoFilter - 1);



				//System.out.println("Numero di attributi nel file prima del filtro (inclusa la bugginess): "+ numAttrNoFilter);
				//System.out.println("Numero di attributi nel file dopo il filtro(inclusa la bugginess): "+ numAttrFiltered);


				//senza e con feature selection
				for (int fs=0;fs<=1;fs++) {

					//fs=1 allora con feature selection
					if(fs==1) {

						//create AttributeSelection object
						AttributeSelection filter = new AttributeSelection();
						//create evaluator and search algorithm objects
						CfsSubsetEval subEval = new CfsSubsetEval();
						GreedyStepwise search = new GreedyStepwise();
						//set the algorithm to search backward
						search.setSearchBackwards(true);
						//set the filter to use the evaluator and search algorithm
						filter.setEvaluator(subEval);
						filter.setSearch(search);

						//specify the dataset
						filter.setInputFormat(noFilterTraining);

						//qui si crea il training filtrato
						filteredTraining = Filter.useFilter(noFilterTraining, filter);

						//stima numero attributi con i filtri
						numAttrFiltered = filteredTraining.numAttributes();

						//evaluation with filtered
						filteredTraining.setClassIndex(numAttrFiltered - 1);
						testingFiltered = Filter.useFilter(testing, filter);
						testingFiltered.setClassIndex(numAttrFiltered - 1);

					}//fine if


					//qui si contano le istanze positive...
					numDefectiveTrain=0;
					numDefectiveTest=0;

					if(fs==0) {

						//ora si contano il numero di buggy nelle Instances
						for(Instance instance: noFilterTraining){

							if(instance.stringValue(numAttrNoFilter-1).equals("YES")) {
								numDefectiveTrain++;
							}
						}
						for(Instance instance: testing){
							if(instance.stringValue(numAttrNoFilter-1).equals("YES")) {
								numDefectiveTest++;
							}
						}
						percentInstOfMajorityClass=2*Math.max(numDefectiveTrain/noFilterTraining.size(),1-numDefectiveTrain/noFilterTraining.size())*100;


					}
					else if(fs==1){
						//ora si contano il numero di buggy nelle Instances
						for(Instance instance: filteredTraining){
							if(instance.stringValue(numAttrFiltered-1).equals("YES")) {
								numDefectiveTrain++;
							}
						}
						for(Instance instance: testingFiltered){
							if(instance.stringValue(numAttrFiltered-1).equals("YES")) {
								numDefectiveTest++;
							}
						}

						percentInstOfMajorityClass=2*Math.max(numDefectiveTrain/filteredTraining.size(),1-numDefectiveTrain/filteredTraining.size())*100;

					}



					//senza balancing o con i tre tipi di balancing			
					for(int balancing=1;balancing<=4;balancing++) {


						//per ogni classificatore
						for(int n=1;n<=3;n++) {
							if(n==1) {

								//NaiveBayes---------------
								NaiveBayes classifier = new NaiveBayes(); //scelgo come classificatore il naive bayes
								myClassificator ="NaiveBayes";
								if(fs==0) {

									classifier.buildClassifier(noFilterTraining); //qui si fa il training non filtrato
									//no resample
									if(balancing==1) {										
										eval =new Evaluation(testing);	
										eval.evaluateModel(classifier, testing);
									}
									//Oversampling
									else if(balancing==2) {

										resample = new Resample();
										resample.setInputFormat(noFilterTraining);
										resample.setNoReplacement(false);
										FilteredClassifier fc = new FilteredClassifier();
										fc.setClassifier(classifier);
										String[] opts = new String[]{ "-B", "1.0", "-Z", ""+percentInstOfMajorityClass+""};
										resample.setOptions(opts);
										fc.setFilter(resample);
										fc.buildClassifier(noFilterTraining);
										eval = new Evaluation(testing);	
										eval.evaluateModel(fc, testing); //sampled
									}

									//undersampling
									else if(balancing==3) {

										resample = new Resample();
										resample.setInputFormat(noFilterTraining);
										FilteredClassifier fc = new FilteredClassifier();
										fc.setClassifier(classifier);
										SpreadSubsample  spreadSubsample = new SpreadSubsample();
										String[] opts = new String[]{ "-M", "1.0"};
										spreadSubsample.setOptions(opts);
										fc.setFilter(spreadSubsample);
										fc.buildClassifier(noFilterTraining);
										eval =new Evaluation(testing);	
										eval.evaluateModel(fc, testing);						               
									}

									else if(balancing==4) {

										resample = new Resample();
										resample.setInputFormat(noFilterTraining);
										FilteredClassifier fc = new FilteredClassifier();
										SMOTE smote = new SMOTE();
										smote.setInputFormat(noFilterTraining);
										fc.setFilter(smote);
										fc.buildClassifier(noFilterTraining);
										eval =new Evaluation(testing);	
										eval.evaluateModel(fc, testing);	

									}


								}
								else if(fs==1) {
									classifier.buildClassifier(filteredTraining); //qui si fa il training filtrato

									if(balancing==1) {
										eval =new Evaluation(testing);
										eval.evaluateModel(classifier, testingFiltered);

									}

									//Oversampling
									else if(balancing==2) {

										resample = new Resample();
										resample.setInputFormat(filteredTraining);
										resample.setNoReplacement(false);
										FilteredClassifier fc = new FilteredClassifier();
										fc.setClassifier(classifier);
										String[] opts = new String[]{ "-B", "1.0", "-Z", ""+percentInstOfMajorityClass+""};
										resample.setOptions(opts);
										fc.setFilter(resample);
										fc.buildClassifier(filteredTraining);
										eval = new Evaluation(testing);	
										eval.evaluateModel(fc, testingFiltered); //sampled
									}
									//undersampling
									else if(balancing==3) {

										resample = new Resample();
										resample.setInputFormat(noFilterTraining);
										FilteredClassifier fc = new FilteredClassifier();
										fc.setClassifier(classifier);
										SpreadSubsample  spreadSubsample = new SpreadSubsample();
										String[] opts = new String[]{ "-M", "1.0"};
										spreadSubsample.setOptions(opts);
										fc.setFilter(spreadSubsample);
										fc.buildClassifier(filteredTraining);
										eval =new Evaluation(testing);	
										eval.evaluateModel(fc, testingFiltered);						               
									}

									else if(balancing==4) {

										resample = new Resample();
										resample.setInputFormat(noFilterTraining);
										FilteredClassifier fc = new FilteredClassifier();
										SMOTE smote = new SMOTE();
										smote.setInputFormat(noFilterTraining);
										fc.setFilter(smote);
										fc.buildClassifier(filteredTraining);
										eval =new Evaluation(testing);	
										eval.evaluateModel(fc, testingFiltered);	

									}								



								}

							}

							else if (n==2) {
								//RandomForest---------------
								RandomForest classifier = new RandomForest(); //scelgo come classificatore RandomForest
								myClassificator ="RandomForest";

								if(fs==0) {
									classifier.buildClassifier(noFilterTraining); //qui si fa il training non filtrato

									//no resample
									if(balancing==1) {
										eval =new Evaluation(testing);	
										eval.evaluateModel(classifier, testing);
									}

									//Oversampling
									else if(balancing==2) {

										resample = new Resample();
										resample.setInputFormat(noFilterTraining);
										resample.setNoReplacement(false);
										FilteredClassifier fc = new FilteredClassifier();
										fc.setClassifier(classifier);
										String[] opts = new String[]{ "-B", "1.0", "-Z", ""+percentInstOfMajorityClass+""};
										resample.setOptions(opts);
										fc.setFilter(resample);
										fc.buildClassifier(noFilterTraining);
										eval = new Evaluation(testing);	
										eval.evaluateModel(fc, testing); //sampled
									}



									//undersampling
									else if(balancing==3) {

										resample = new Resample();
										resample.setInputFormat(noFilterTraining);
										FilteredClassifier fc = new FilteredClassifier();
										fc.setClassifier(classifier);
										SpreadSubsample  spreadSubsample = new SpreadSubsample();
										String[] opts = new String[]{ "-M", "1.0"};
										spreadSubsample.setOptions(opts);
										fc.setFilter(spreadSubsample);
										fc.buildClassifier(noFilterTraining);
										eval =new Evaluation(testing);	
										eval.evaluateModel(fc, testing);						               
									}


									else if(balancing==4) {

										resample = new Resample();
										resample.setInputFormat(noFilterTraining);
										FilteredClassifier fc = new FilteredClassifier();
										SMOTE smote = new SMOTE();
										smote.setInputFormat(noFilterTraining);
										fc.setFilter(smote);
										fc.buildClassifier(noFilterTraining);
										eval =new Evaluation(testing);	
										eval.evaluateModel(fc, testing);	

									}
								}
								else if(fs==1){
									classifier.buildClassifier(filteredTraining); //qui si fa il training filtrato

									if(balancing==1) {
										eval =new Evaluation(testing);
										eval.evaluateModel(classifier, testingFiltered);

									}

									//Oversampling
									else if(balancing==2) {

										resample = new Resample();
										resample.setInputFormat(filteredTraining);
										resample.setNoReplacement(false);
										FilteredClassifier fc = new FilteredClassifier();
										fc.setClassifier(classifier);
										String[] opts = new String[]{ "-B", "1.0", "-Z", ""+percentInstOfMajorityClass+""};
										resample.setOptions(opts);
										fc.setFilter(resample);
										fc.buildClassifier(filteredTraining);
										eval = new Evaluation(testing);	
										eval.evaluateModel(fc, testingFiltered); //sampled
									}

									//undersampling
									else if(balancing==3) {

										resample = new Resample();
										resample.setInputFormat(noFilterTraining);
										FilteredClassifier fc = new FilteredClassifier();
										fc.setClassifier(classifier);
										SpreadSubsample  spreadSubsample = new SpreadSubsample();
										String[] opts = new String[]{ "-M", "1.0"};
										spreadSubsample.setOptions(opts);
										fc.setFilter(spreadSubsample);
										fc.buildClassifier(filteredTraining);
										eval =new Evaluation(testing);	
										eval.evaluateModel(fc, testingFiltered);						               
									}

									else if(balancing==4) {

										resample = new Resample();
										resample.setInputFormat(noFilterTraining);
										FilteredClassifier fc = new FilteredClassifier();
										SMOTE smote = new SMOTE();
										smote.setInputFormat(filteredTraining);
										fc.setFilter(smote);
										fc.buildClassifier(filteredTraining);
										eval =new Evaluation(testing);	
										eval.evaluateModel(fc, testingFiltered);	

									}
								} 
							}
							else if (n==3) {
								//Ibk---------------
								IBk classifier = new IBk(); //scelgo come classificatore Ibk
								myClassificator ="IBk";
								if(fs==0) {
									classifier.buildClassifier(noFilterTraining); //qui si fa il training non filtrato

									//no resample
									if(balancing==1) {
										eval =new Evaluation(testing);	
										eval.evaluateModel(classifier, testing);
									}

									//Oversampling
									else if(balancing==2) {

										resample = new Resample();
										resample.setInputFormat(noFilterTraining);
										resample.setNoReplacement(false);
										FilteredClassifier fc = new FilteredClassifier();
										fc.setClassifier(classifier);
										String[] opts = new String[]{ "-B", "1.0", "-Z", ""+percentInstOfMajorityClass+""};
										resample.setOptions(opts);
										fc.setFilter(resample);
										fc.buildClassifier(noFilterTraining);
										eval = new Evaluation(testing);	
										eval.evaluateModel(fc, testing); //sampled
									}

									//undersampling
									else if(balancing==3) {

										resample = new Resample();
										resample.setInputFormat(noFilterTraining);
										FilteredClassifier fc = new FilteredClassifier();
										fc.setClassifier(classifier);
										SpreadSubsample  spreadSubsample = new SpreadSubsample();
										String[] opts = new String[]{ "-M", "1.0"};
										spreadSubsample.setOptions(opts);
										fc.setFilter(spreadSubsample);
										fc.buildClassifier(noFilterTraining);
										eval =new Evaluation(testing);	
										eval.evaluateModel(fc, testing);						               
									}

									else if(balancing==4) {

										resample = new Resample();
										resample.setInputFormat(noFilterTraining);
										FilteredClassifier fc = new FilteredClassifier();
										SMOTE smote = new SMOTE();
										smote.setInputFormat(noFilterTraining);
										fc.setFilter(smote);
										fc.buildClassifier(noFilterTraining);
										eval =new Evaluation(testing);	
										eval.evaluateModel(fc, testing);	

									}




								}
								else if(fs==1){
									classifier.buildClassifier(filteredTraining); //qui si fa il training filtrato

									if(balancing==1) {
										eval =new Evaluation(testing);
										eval.evaluateModel(classifier, testingFiltered);

									}

									//Oversampling
									else if(balancing==2) {

										resample = new Resample();
										resample.setInputFormat(filteredTraining);
										resample.setNoReplacement(false);
										FilteredClassifier fc = new FilteredClassifier();
										fc.setClassifier(classifier);
										String[] opts = new String[]{ "-B", "1.0", "-Z", ""+percentInstOfMajorityClass+""};
										resample.setOptions(opts);
										fc.setFilter(resample);
										fc.buildClassifier(filteredTraining);
										eval = new Evaluation(testing);	
										eval.evaluateModel(fc, testingFiltered); //sampled
									}

									//undersampling
									else if(balancing==3) {

										resample = new Resample();
										resample.setInputFormat(noFilterTraining);
										FilteredClassifier fc = new FilteredClassifier();
										fc.setClassifier(classifier);
										SpreadSubsample  spreadSubsample = new SpreadSubsample();
										String[] opts = new String[]{ "-M", "1.0"};
										spreadSubsample.setOptions(opts);
										fc.setFilter(spreadSubsample);
										fc.buildClassifier(filteredTraining);
										eval =new Evaluation(testing);	
										eval.evaluateModel(fc, testingFiltered);						               
									}

									else if(balancing==4) {

										resample = new Resample();
										resample.setInputFormat(noFilterTraining);
										FilteredClassifier fc = new FilteredClassifier();
										SMOTE smote = new SMOTE();
										smote.setInputFormat(filteredTraining);
										fc.setFilter(smote);
										fc.buildClassifier(filteredTraining);
										eval =new Evaluation(testing);	
										eval.evaluateModel(fc, testingFiltered);	

									}


								} 
							}




							//--------------------------------------------------------------
							//ora si scrive file csv coi risultati

							//se è la prima iterazione
							if( fileWriter==null) {

								String name = ProjectName+" Deliverable 2 Milestone 3.csv";

								//True = Append to file, false = Overwrite
								fileWriter = new FileWriter(name,true);
								fileWriter.append("Dataset,#Training Release,%Training,%Defective in training,"
										+ "%Defective in testing,classifier,balancing,Feature Selection,TP,FP,TN,FN,"
										+ "Precision,Recall,ROC Area, Kappa");

								fileWriter.append("\n");
							}

							//System.out.println((double)noFilterTraining.size()/(double)(testing.size()+noFilterTraining.size()));
							//System.out.println(noFilterTraining.size()+testing.size());
							fileWriter.append(ProjectName);
							fileWriter.append(",");
							fileWriter.append(String.valueOf(version-1));
							fileWriter.append(",");
							fileWriter.append(String.valueOf((String.format("%.3f", (double) (noFilterTraining.size()/(double)(testing.size()+noFilterTraining.size()))))).replace(',', '.'));//modifica con sampling
							fileWriter.append(",");
							fileWriter.append(String.valueOf((String.format("%.3f",(double)numDefectiveTrain/(double)noFilterTraining.size()))).replace(',', '.'));
							fileWriter.append(",");
							fileWriter.append(String.valueOf((String.format("%.3f",(double)numDefectiveTest/(double)testing.size()))).replace(',', '.'));
							fileWriter.append(",");
							fileWriter.append(myClassificator);
							fileWriter.append(",");
							fileWriter.append(String.valueOf(String.valueOf(balancing)));
							fileWriter.append(",");
							fileWriter.append(String.valueOf(fs));
							fileWriter.append(",");
							fileWriter.append(String.valueOf((int)eval.numTruePositives(1)));
							fileWriter.append(",");
							fileWriter.append(String.valueOf((int)eval.numFalsePositives(1)));
							fileWriter.append(",");
							fileWriter.append(String.valueOf((int)eval.numTrueNegatives(1)));
							fileWriter.append(",");
							fileWriter.append(String.valueOf((int)eval.numFalseNegatives(1)));
							fileWriter.append(",");
							fileWriter.append(String.valueOf(numberFormat.format(eval.precision(1)).replace(',', '.')));
							fileWriter.append(",");
							fileWriter.append(String.valueOf(numberFormat.format(eval.recall(1)).replace(',', '.')));
							fileWriter.append(",");
							fileWriter.append(String.valueOf(numberFormat.format(eval.areaUnderROC(1)).replace(',', '.')));
							fileWriter.append(",");
							fileWriter.append(String.valueOf(numberFormat.format(eval.kappa()).replace(',', '.')));
							fileWriter.append("\n");

						}//per ogni classificatore

					}//per ogni sampling
				}//per ogni fs
			}//per ogni versione
			fileWriter.flush();
			fileWriter.close();
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1); ;
			// TODO: handle exception
		}
		
	}

}
