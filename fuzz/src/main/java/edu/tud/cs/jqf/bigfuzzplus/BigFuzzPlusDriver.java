package edu.tud.cs.jqf.bigfuzzplus;

import edu.berkeley.cs.jqf.fuzz.junit.GuidedFuzzing;
import edu.tud.cs.jqf.bigfuzzplus.stackedMutation.StackedMutation;
import edu.tud.cs.jqf.bigfuzzplus.stackedMutation.StackedMutationEnum;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static edu.tud.cs.jqf.bigfuzzplus.systematicMutation.SystematicMutation.*;

public class BigFuzzPlusDriver {
	// These booleans are for debugging purposes only, toggle them if you want to see the information
	public static boolean PRINT_METHOD_NAMES = false;
	public static boolean PRINT_MUTATION_DETAILS = false;
    public static boolean PRINT_COVERAGE_DETAILS = false;
    public static boolean PRINT_INPUT_SELECTION_DETAILS = false;
    public static boolean LOG_AND_PRINT_STATS = false;
	public static boolean PRINT_ERRORS = false;
	public static boolean PRINT_MUTATIONS = false;
	public static boolean PRINT_TEST_RESULTS = false;
	public static boolean SAVE_INPUTS = false;

    // ---------- MANUAL VARIABLES ------------
    /** Cleans outputDirectory if true, else adds a new subdirectory in which the results are stored */
    public static boolean CLEAR_ALL_PREVIOUS_RESULTS_ON_START = false;
    public static boolean SAVE_UNIQUE_FAILURES = false;
	public static int NUMBER_OF_ITERATIONS = 1;
	public static Duration maxDuration = Duration.of(10, ChronoUnit.MINUTES);
	public static SelectionMethod selection = SelectionMethod.COVERAGE_FILES;
	/** Favor rate is used to tweak boosted grey-box fuzzing. Only relevant if selection = COVERAGE_FILES.
	 * 0 = only baseline selection. 1 = only boosted grey-box fuzzing. */
	public static double favorRate = 0;


	public static BigFuzzPlusLog log = BigFuzzPlusLog.getInstance();

    /**
     * Run the BigFuzzPlus program with the following parameters for StackedMutation:
     * [0] - test class
     * [1] - test method
     * [2] - mutation method           (StackedMutation)
     * [3] - max Trials                (default = Long.MAXVALUE)
     * [4] - BigFuzz / TabFuzz
     *          0= BigFuzz
     *          1= TabFuzz
     * [5] - stacked mutation method   (default = 0)
     *          0 = Disabled
     *          1 = Permute_random (permute between 1 and the max amount of mutations)
     *          2 = Permute_max (Always permute until the max amount of mutations)
     *          3 = Smart_stack (Apply higher-order mutation exclusion rules)
     *          4 = Single mutate (Only apply 1 mutation per column)
     * [6] - max mutation stack        (default = 2)
     *
     * * Run the BigFuzzPlus program with the following parameters for SystematicMutation:
     * [0] - test class
     * [1] - test method
     * [2] - mutation method           (SystematicMutation)
     * [3] - max Trials                (default = Long.MAXVALUE)
     * [4] - mutate columns            (default = disabled)
     * [5] - max mutation depth        (default = 6)
     *
     * @param args program arguments
     */
    public static void main(String[] args) {

        // LOAD PROGRAM ARGUMENTS
        if (args.length < 3) {
            System.err.println("Usage: java " + BigFuzzPlusDriver.class + " TEST_CLASS TEST_METHOD MUTATION_CLASS [MAX_TRIALS]");
            System.exit(1);
        }

		String testClassName = args[0];
		String testMethodName = args[1];
		String mutationMethodClassName = args[2];

		long maxTrials = args.length > 3 ? Long.parseLong(args[3]) : Long.MAX_VALUE;

		long programStartTime = System.currentTimeMillis();
        File allOutputDir = new File("fuzz-results");
        File outputDir = new File(allOutputDir, "" + programStartTime + " - " + testClassName + " - " + mutationMethodClassName );
        if (!allOutputDir.exists() && !allOutputDir.mkdir()) {
            System.err.println("Something went wrong with making the output directory for this run: " + allOutputDir);
            System.exit(0);
        }
        if (!outputDir.mkdir()) {
            System.err.println("Something went wrong with making the output directory for this run: " + outputDir);
            System.exit(0);
        }
        if (CLEAR_ALL_PREVIOUS_RESULTS_ON_START && allOutputDir.isDirectory()) {
            try {
                FileUtils.cleanDirectory(allOutputDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

		StackedMutationEnum.StackedMutationMethod stackedMutationMethod = StackedMutationEnum.StackedMutationMethod.Disabled;
        int intMutationStackCount = 0;
		if (mutationMethodClassName.equalsIgnoreCase("stackedmutation")) {
			int intStackedMutationMethod = args.length > 4 ? Integer.parseInt(args[4]) : 0;
			// This variable is used for the stackedMutationMethod: Smart_mutate
			// If the selected stackedMutationMethod is smart_mutate and this argument is not given, default is set to 2. If smart_mutate is not selected, set to 0
			stackedMutationMethod = StackedMutationEnum.intToStackedMutationMethod(intStackedMutationMethod);
			intMutationStackCount = args.length > 5 ? Integer.parseInt(args[5]) : stackedMutationMethod == StackedMutationEnum.StackedMutationMethod.Smart_stack ? 2 : 0;
			System.out.println("stackedMutationMethod: " + stackedMutationMethod);
			log.logProgramArgumentsStackedMutation(testClassName, testMethodName, mutationMethodClassName, stackedMutationMethod, intMutationStackCount, outputDir, programStartTime);
		}
		if (mutationMethodClassName.equalsIgnoreCase("systematicmutation")) {
			if (args.length > 4) {
				MUTATE_COLUMNS = Boolean.parseBoolean(args[4]);
				System.out.println("Mutate columns: " + MUTATE_COLUMNS);
			} if (args.length > 5) {
				MUTATION_DEPTH = Integer.parseInt(args[5]);
				System.out.println("Mutation depth: " + MUTATION_DEPTH);
			}
			log.logProgramArgumentsSystematicMutation(testClassName, testMethodName, mutationMethodClassName, MUTATE_COLUMNS, MUTATION_DEPTH, outputDir, programStartTime);
		}
		if (mutationMethodClassName.equalsIgnoreCase("random")) {
			MUTATE_RANDOM = true;
			System.out.println("Mutating randomly");
			log.logProgramArgumentsSystematicMutation(testClassName, testMethodName, mutationMethodClassName, MUTATE_COLUMNS, MUTATION_DEPTH, outputDir, programStartTime);
		}
        else {
            log.logProgramArguments(testClassName,testMethodName,mutationMethodClassName,outputDir,programStartTime);
        }


		// **************

        log.printProgramArguments();

		String file;
		switch (testClassName) {
			case "WordCountDriver":
			case "WordCountNewDriver":
				file = "dataset/conf_wordcount";
				break;
			case "CommuteTypeDriver":
				file = "dataset/commutetype";
				break;
			case "ExternalUDFDriver":
				file = "dataset/conf_externaludf";
				break;
			case "FindSalaryDriver":
				file = "dataset/conf_findsalary";
				break;
			case "StudentGradesDriver":
				file = "dataset/conf_studentgrades";
				break;
			case "MovieRatingDriver":
				file = "dataset/conf_movierating";
				break;
			case "SalaryAnalysisDriver":
				file = "dataset/conf_salary";
				break;
			case "PropertyDriver":
				file = "dataset/conf_property";
				break;
			default:
				file = "dataset/conf";
		}

	    for (int i = 0; i < NUMBER_OF_ITERATIONS; i++) {
	        int atIteration = i + 1;
	        System.out.println("\n******** START OF PROGRAM ITERATION: " + atIteration + "**********************");

            long iterationStartTime = System.currentTimeMillis();
            String iterationOutputDir = outputDir + "/Test" + atIteration;

            try {
                File itOutputDir = new File(iterationOutputDir);
                BigFuzzPlusGuidance guidance = new BigFuzzPlusGuidance("Test" + atIteration, file, maxTrials, maxDuration, itOutputDir, mutationMethodClassName, favorRate, selection);

                // Set the provided input argument stackedMutationMethod in the guidance mutation
                if(guidance.mutation instanceof StackedMutation) {
                    ((StackedMutation)guidance.mutation).setStackedMutationMethod(stackedMutationMethod);
                    ((StackedMutation)guidance.mutation).setMutationStackCount(intMutationStackCount);
                }

                // Set the randomization seed to the program start time. Seed is passed to allow for custom seeds, independent of the program start time
                guidance.setRandomizationSeed(iterationStartTime);

                // Set the test class name in the guidance for the failure tracking
                guidance.setTestClassName(testClassName);

                // Run the Junit test
                GuidedFuzzing.run(testClassName, testMethodName, guidance, System.out);
                long endTime = System.currentTimeMillis();

                // Evaluate the results
                log.evaluation(testClassName, testMethodName, file, maxTrials, maxDuration, iterationStartTime, endTime, guidance, atIteration);
                log.writeToLists(guidance, maxTrials);
                log.addDuration(endTime - iterationStartTime);
                System.out.println("************************* END OF PROGRAM ITERATION ************************");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        log.summarizeProgramIterations();
        log.writeLogToFile(outputDir);
    }
}
