package logic.example;

import association.Association;
import association.HashAssociation;
import vector.Vector;
import logic.bias.Type;
import logic.expression.formula.Predicate;
import logic.expression.formula.PredicateInstance;
import logic.expression.term.Constant;
import logic.expression.term.Term;
import logic.expression.visitor.ExpressionLogicPrinter;
import logic.theory.Structure;
import logic.theory.StructureBuilder;

import java.util.Set;

/**
 * Created by samuelkolb on 11/11/14.
 *
 * @author Samuel Kolb
 */
public class Example {

	//region Variables
	private final Setup setup;

	public Setup getSetup() {
		return setup;
	}

	private final Vector<PredicateInstance> instances;
	//endregion

	//region Construction

	public Example(Setup setup, Vector<PredicateInstance> instances) {
		this.setup = setup;
		this.instances = instances;
		Set<Predicate> predicates = setup.getPredicates();
		for(PredicateInstance instance : instances)
			if(!predicates.contains(instance.getPredicate()))
				throw new IllegalArgumentException("Instances predicate not in setup");
			else if(!instance.isGround())
				throw new IllegalArgumentException("Examples cannot contain ungrounded elements");
	}

	//endregion

	//region Public methods

	public Structure getStructure() {
		StructureBuilder builder = new StructureBuilder();
		buildTypes(builder);
		buildPredicates(builder);
		return builder.create();
	}

	private void buildTypes(StructureBuilder builder) {
		Association<Type, Constant> typeAssociation = new HashAssociation<>();
		buildConstants(typeAssociation);
		for(Type type : getSetup().getTypes())
			if(typeAssociation.containsKey(type))
				builder.addConstants(type, typeAssociation.getValues(type));
			else
				builder.addEmptyType(type);
	}

	private void buildPredicates(StructureBuilder builder) {
		Association<Predicate, PredicateInstance> predicateAssociation = new HashAssociation<>();
		buildPredicates(predicateAssociation);
		for(Predicate predicate : getSetup().getPredicates())
			if(predicateAssociation.containsKey(predicate))
				builder.addPredicateInstances(predicate, predicateAssociation.getValues(predicate));
			else
				builder.addEmptyPredicate(predicate);
	}

	private void buildPredicates(Association<Predicate, PredicateInstance> association) {
		for(PredicateInstance instance : instances)
			association.associate(instance.getPredicate(), instance);
	}

	private void buildConstants(Association<Type, Constant> constants) {
		for(PredicateInstance instance : instances)
			for(Term term : instance.getTerms())
				if(term instanceof Constant) {
					Constant constant = (Constant) term;
					constants.associate(constant.getType(), constant);
				}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("Example:");
		for(PredicateInstance instance : instances)
			builder.append(" ").append(ExpressionLogicPrinter.print(instance));
		return builder.toString();
	}

	//endregion
}
