package clausal_discovery;

import idp.IdpExecutor;
import idp.IdpExpressionPrinter;
import log.Log;
import logic.example.Example;
import logic.expression.formula.Formula;
import logic.theory.LogicProgram;
import logic.theory.Structure;
import logic.theory.Theory;
import vector.*;
import vector.Vector;

import java.util.*;

/**
 * Created by samuelkolb on 02/03/15.
 *
 * @author Samuel Kolb
 */
public class ValidityCalculator {

	//region Variables
	private final LogicBase base;

	private final List<Formula> formulas = new ArrayList<>();

	private final Map<Formula, Boolean> validityTable = new HashMap<>();
	//endregion

	//region Construction

	public ValidityCalculator(LogicBase base) {
		this.base = base;
	}

	//endregion

	//region Public methods
	public void submitFormula(Formula formula) {
		Log.LOG.printLine("INFO Submitted " + IdpExpressionPrinter.print(formula));
		if(!validityTable.containsKey(formula))
			formulas.add(formula);
	}

	public boolean isValid(Formula formula) {
		Log.LOG.printLine("INFO Is valid? " + IdpExpressionPrinter.print(formula));
		if(validityTable.containsKey(formula))
			return validityTable.get(formula);
		extendValidityTable();
		return validityTable.get(formula);
	}

	private void extendValidityTable() {
		Log.LOG.printLine("Calculating...");
		Vector<Structure> structures = new WriteOnceVector<>(new Structure[base.getExamples().size()]);
		for(Example example : base.getExamples())
			structures.add(example.getStructure());
		Vector<Theory> theories = new WriteOnceVector<>(new Theory[formulas.size()]);
		for(Formula formula : formulas)
			theories.add(new Theory(formula));
		LogicProgram program = new LogicProgram(base.getVocabulary(), theories, structures);
		boolean[] validity = IdpExecutor.get().areValid(program);
		for(int i = 0; i < formulas.size(); i++)
			validityTable.put(formulas.get(i), validity[i]);
		formulas.clear();
		Log.LOG.printLine("...Done");
	}
	//endregion
}