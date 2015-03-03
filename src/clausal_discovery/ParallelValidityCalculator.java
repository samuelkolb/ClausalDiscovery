package clausal_discovery;

import logic.expression.formula.Formula;
import logic.theory.LogicExecutor;
import logic.theory.LogicProgram;
import logic.theory.Structure;
import logic.theory.Theory;
import vector.Vector;

import java.util.Map;
import java.util.concurrent.*;

/**
 * The parallel validity calculator starts calculating validity in parallel when a request is submitted. When the
 * validity status is queried it returns the calculated result or waits until it is available.
 *
 * @author Samuel Kolb
 */
public class ParallelValidityCalculator extends ValidityCalculator {

	private class CheckValidCallable implements Callable<Boolean> {

		private final Formula formula;

		private final Vector<Structure> structures;

		private CheckValidCallable(Formula formula, Vector<Structure> structures) {
			this.formula = formula;
			this.structures = structures;
		}

		@Override
		public Boolean call() throws Exception {
			Vector<Theory> theories = new Vector<Theory>(new Theory(formula));
			LogicProgram program = new LogicProgram(getBase().getVocabulary(), theories, structures);
			return getExecutor().isValid(program);
		}
	}

	//region Variables
	private final Map<Formula, Future<Boolean>> validityTable = new ConcurrentHashMap<>();

	private final ExecutorService executorService = Executors.newFixedThreadPool(8);
	//endregion

	//region Construction

	/**
	 * Creates a new parallel validity calculator
	 * @param base		The logic base
	 * @param executor	The executor to be used for validity tests
	 */
	public ParallelValidityCalculator(LogicBase base, LogicExecutor executor) {
		super(base, executor);
	}

	//endregion

	//region Public methods

	@Override
	public void submitFormula(Formula formula) {
		if(!validityTable.containsKey(formula))
			validityTable.put(formula, executorService.submit(new CheckValidCallable(formula, getStructures())));
	}

	@Override
	public boolean isValid(Formula formula) {
		try {
			return validityTable.get(formula).get();
		} catch(InterruptedException | ExecutionException e) {
			throw new IllegalStateException(e);
		}
	}

	//endregion
}