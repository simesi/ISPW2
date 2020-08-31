package it.uniroma2.isw2.progetto;
import weka.core.Instance;
import weka.core.Instances;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
import weka.classifiers.lazy.IBk;


public class Weka {

	private static final String TRAINING_FOR_RELEASE =" Training for Release "; 
	private static final String TESTING_FOR_RELEASE =" Testing for Release ";
	private static final String ARFF=".arff";
	//questo metodo compara i risultati dei tre classificatori utilizzando la tecnica WalkForward
	public void doClassificationMilestone2(int maxVersion, String projectName) {

		String myClassificator=null;
		Evaluation eval = null;
		DecimalFormat numberFormat = new DecimalFormat("0.00");

		for(int version=2;version<=maxVersion;version++) {

			String arffNameFileTrain = "";
			String arffNameFileTest = "";

			//prima ci si crea un file arff da quello csv
			try {
				// load CSV
				CSVLoader loader = new CSVLoader();
				loader.setSource(new File(projectName+TRAINING_FOR_RELEASE+version+".csv"));
				Instances data = loader.getDataSet();

				// save ARFF
				ArffSaver saver = new ArffSaver();
				saver.setInstances(data);


				arffNameFileTrain = projectName+TRAINING_FOR_RELEASE+version+ARFF;


				saver.setFile(new File(arffNameFileTrain));
				saver.writeBatch();


			} catch (IOException e) {
				e.printStackTrace();
				System.exit(-1);
			}



			//adesso ci si crea l'arff per il testing
			try {
				// load CSV
				CSVLoader loader = new CSVLoader();
				loader.setSource(new File(projectName+TESTING_FOR_RELEASE+version+".csv"));
				Instances data = loader.getDataSet();

				// save ARFF
				ArffSaver saver = new ArffSaver();
				saver.setInstances(data);


				arffNameFileTest = projectName +TESTING_FOR_RELEASE+version+ARFF;


				saver.setFile(new File(arffNameFileTest));
				saver.writeBatch();

			} catch (IOException e) {
				e.printStackTrace();
				System.exit(-1);
			}

			String name = projectName+" Deliverable 2 Milestone 2.csv";
			//ora si classifica ------------------------- 

			try (   	//True = Append to file, false = Overwrite
					FileWriter fileWriter = new FileWriter(name,true);
					)
			{
				fileWriter.append("Dataset,#Training Release, Classifier, Precision, Recall, AUC, KAPPA");
				fileWriter.append("\n");

				//load datasets
				DataSource source1 = new DataSource(arffNameFileTrain);
				Instances training = source1.getDataSet();

				DataSource source2 = new DataSource(arffNameFileTest);
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



					fileWriter.append(projectName);
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
				System.exit(-1);
			} 

		}

	}


	public void doClassificationMilestone3(int maxversion, String projectName) {


		String myClassificator=null;
		Evaluation eval = null;
		DecimalFormat numberFormat = new DecimalFormat("0.00");
		int numAttrFiltered=0;
		int numAttrNoFilter=0;
		int numDefectiveTrain=0;
		int numDefectiveTest=0;
		int percentInstOfMajorityClass=0;
		Resample resample= null;
		String name = projectName+" Deliverable 2 Milestone 3.csv";

		try 
		{



			for(int version=2;version<=maxversion;version++) {


				DataSource source = new DataSource(projectName +TRAINING_FOR_RELEASE+version+ARFF);

				DataSource source2 = new DataSource(projectName +TESTING_FOR_RELEASE+version+ARFF);

				Instances noFilterTraining = source.getDataSet();
				Instances testing = source2.getDataSet();
				Instances filteredTraining = null;
				Instances testingFiltered = null;

				//stima senza filtri
				numAttrNoFilter = noFilterTraining.numAttributes();
				noFilterTraining.setClassIndex(numAttrNoFilter - 1);
				testing.setClassIndex(numAttrNoFilter - 1);


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


						//qui si contano le istanze positive...
						 percentInstOfMajorityClass=calculateDefectiveInInstances(filteredTraining,testingFiltered,numAttrFiltered);




					}//fine if


					if(fs==0) {

						//qui si contano le istanze positive...
						 percentInstOfMajorityClass= calculateDefectiveInInstances(noFilterTraining,testing,numAttrNoFilter);


						

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

							try (
									//True = Append to file, false = Overwrite
									FileWriter fileWriter = new FileWriter(name,true);
									)
							{
								fileWriter.append("Dataset,#Training Release,%Training,%Defective in training,"
										+ "%Defective in testing,classifier,balancing,Feature Selection,TP,FP,TN,FN,"
										+ "Precision,Recall,ROC Area, Kappa");

								fileWriter.append("\n");

								fileWriter.append(projectName);
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
							}
							catch (IOException e) {
								e.printStackTrace();
								System.exit(-1); 
							}
						}//per ogni classificatore

					}//per ogni sampling
				}//per ogni fs
			}//per ogni versione
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1); 
		}



	}


	private int calculateDefectiveInInstances(Instances train, Instances test, int numAttrFiltered) {
		int numDefectiveTrain=0;
		int numDefectiveTest=0;

		//ora si contano il numero di buggy nelle Instances
		for(Instance instance: train){
			if(instance.stringValue(numAttrFiltered-1).equals("YES")) {
				numDefectiveTrain++;
			}
		}
		for(Instance instance: test){
			if(instance.stringValue(numAttrFiltered-1).equals("YES")) {
				numDefectiveTest++;
			}
		}

		return 2*Math.max(numDefectiveTrain/train.size(),1-numDefectiveTrain/train.size())*100;

		
	}

}
