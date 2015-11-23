package logic.expression.formula;

import basic.ArrayUtil;
import basic.StringUtil;
import logic.bias.Type;
import logic.expression.term.Term;
import vector.SafeList;

import java.util.List;

/**
 * Created by samuelkolb on 22/10/14.
 *
 * @author Samuel Kolb
 */
public class Predicate {

	//region Variables

	// IVAR name - The predicate name

	private final String name;

	public String getName() {
		return name;
	}

	// IVAR types - The argument types

	private final SafeList<Type> types;

	public SafeList<Type> getTypes() {
		return types;
	}

	//endregion

	//region Construction

	/**
	 * Creates a new non-symmetric predicate with the given name and arity
	 * The modes and types of the parameters will be undefined
	 * @param name	The predicate name
	 * @param arity	The arity (number of parameters) of the predicate
	 */
	public Predicate(String name, int arity) {
		this(name, ArrayUtil.fill(new Type[arity], Type.GENERIC));
	}

	/**
	 * Creates a new predicate
	 * @param name		The predicate name
	 * @param types		The types of the parameters
	 */
	public Predicate(String name, Type... types) {
		this.name = name;
		this.types = SafeList.from(types);
	}

	/**
	 * Creates a new predicate.
	 * @param name	The name of the predicate
	 * @param types	The types of the predicate
	 */
	public Predicate(String name, List<Type> types) {
		this.name = name;
		this.types = SafeList.from(types);
	}

	//endregion

	//region Public methods

	/**
	 * Creates and returns an instance of this predicate
	 * @param terms The terms to instantiate the predicate with<br />
	 *              The number of terms must match the arity of this predicate
	 * @return  An instance of this predicate
	 */
	public PredicateInstance getInstance(Term... terms) {
		return new PredicateInstance(this, terms);
	}

	/**
	 * Creates and returns an instance of this predicate.
	 * @param terms The terms to instantiate the predicate with, the amount of terms must match the arity of this
	 *              predicate
	 * @return  An instance of this predicate
	 */
	public PredicateInstance getInstance(List<Term> terms) {
		return new PredicateInstance(this, terms);
	}

	public int getArity() {
		return types.size();
	}

	@Override
	public String toString() {
		return getName() + "(" + StringUtil.join(", ", getTypes()) + ")";
	}

	@Override
	public boolean equals(Object o) {
		if(this == o) return true;
		if(o == null || getClass() != o.getClass()) return false;

		Predicate predicate = (Predicate) o;

		return name.equals(predicate.name) && types.equals(predicate.types);

	}

	@Override
	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + types.hashCode();
		return result;
	}

	//endregion
}
