package LexicalAnalyzer.FSA;

import java.io.Serializable;
import java.util.*;

/**
 * Basic automata operations.
 */
final public class BasicOperations {
	
	private BasicOperations() {}

	/** 
	 * Returns an automaton that accepts the concatenation of the languages of 
	 * the given automata. 
	 * <p>
	 * Complexity: linear in number of states. 
	 */
	static public Automaton concatenate(Automaton a1, Automaton a2) {
		if (a1.isSingleton() && a2.isSingleton())
			return BasicAutomata.makeString(a1.singleton + a2.singleton);
		if (isEmpty(a1) || isEmpty(a2))
			return BasicAutomata.makeEmpty();
		boolean deterministic = a1.isSingleton() && a2.isDeterministic();
		if (a1 == a2) {
			a1 = a1.cloneExpanded();
			a2 = a2.cloneExpanded();
		} else {
			a1 = a1.cloneExpandedIfRequired();
			a2 = a2.cloneExpandedIfRequired();
		}
		for (State s : a1.getAcceptStates()) {
			s.accept = false;
			s.addEpsilon(a2.initial);
		}
		a1.deterministic = deterministic;
		a1.clearHashCode();
		a1.checkMinimizeAlways();
		return a1;
	}
	
	/**
	 * Returns an automaton that accepts the concatenation of the languages of
	 * the given automata.
	 * <p>
	 * Complexity: linear in total number of states.
	 */
	static public Automaton concatenate(List<Automaton> l) {
		if (l.isEmpty())
			return BasicAutomata.makeEmptyString();
		boolean all_singleton = true;
		for (Automaton a : l)
			if (!a.isSingleton()) {
				all_singleton = false;
				break;
			}
		if (all_singleton) {
			StringBuilder b = new StringBuilder();
			for (Automaton a : l)
				b.append(a.singleton);
			return BasicAutomata.makeString(b.toString());
		} else {
			for (Automaton a : l)
				if (a.isEmpty())
					return BasicAutomata.makeEmpty();
			Set<Integer> ids = new HashSet<Integer>();
			for (Automaton a : l)
				ids.add(System.identityHashCode(a));
			boolean has_aliases = ids.size() != l.size();
			Automaton b = l.get(0);
			if (has_aliases)
				b = b.cloneExpanded();
			else
				b = b.cloneExpandedIfRequired();
			Set<State> ac = b.getAcceptStates();
			boolean first = true;
			for (Automaton a : l)
				if (first)
					first = false;
				else {
					if (a.isEmptyString())
						continue;
					Automaton aa = a;
					if (has_aliases)
						aa = aa.cloneExpanded();
					else
						aa = aa.cloneExpandedIfRequired();
					Set<State> ns = aa.getAcceptStates();
					for (State s : ac) {
						s.accept = false;
						s.addEpsilon(aa.initial);
						if (s.accept)
							ns.add(s);
					}
					ac = ns;
				}
			b.deterministic = false;
			b.clearHashCode();
			b.checkMinimizeAlways();
			return b;
		}
	}

	/**
	 * Returns an automaton that accepts the union of the empty string and the
	 * language of the given automaton.
	 * <p>
	 * Complexity: linear in number of states.
	 */
	static public Automaton optional(Automaton a) {
		a = a.cloneExpandedIfRequired();
		State s = new State();
		s.addEpsilon(a.initial);
		s.accept = true;
		a.initial = s;
		a.deterministic = false;
		a.clearHashCode();
		a.checkMinimizeAlways();
		return a;
	}
	
	/**
	 * Returns an automaton that accepts the Kleene star (zero or more
	 * concatenated repetitions) of the language of the given automaton.
	 * Never modifies the input automaton language.
	 * <p>
	 * Complexity: linear in number of states.
	 */
	static public Automaton repeat(Automaton a) {
		a = a.cloneExpanded();
		State s = new State();
		s.accept = true;
		s.addEpsilon(a.initial);
		for (State p : a.getAcceptStates())
			p.addEpsilon(s);
		a.initial = s;
		a.deterministic = false;
		a.clearHashCode();
		a.checkMinimizeAlways();
		return a;
	}

	/**
	 * Returns an automaton that accepts <code>min</code> or more
	 * concatenated repetitions of the language of the given automaton.
	 * <p>
	 * Complexity: linear in number of states and in <code>min</code>.
	 */
	static public Automaton repeat(Automaton a, int min) {
		if (min == 0)
			return repeat(a);
		List<Automaton> as = new ArrayList<Automaton>();
		while (min-- > 0)
			as.add(a);
		as.add(repeat(a));
		return concatenate(as);
	}
	
	/**
	 * Returns an automaton that accepts between <code>min</code> and
	 * <code>max</code> (including both) concatenated repetitions of the
	 * language of the given automaton.
	 * <p>
	 * Complexity: linear in number of states and in <code>min</code> and
	 * <code>max</code>.
	 */
	static public Automaton repeat(Automaton a, int min, int max) {
		if (min > max)
			return BasicAutomata.makeEmpty();
		max -= min;
		a.expandSingleton();
		Automaton b;
		if (min == 0)
			b = BasicAutomata.makeEmptyString();
		else if (min == 1)
			b = a.clone();
		else {
			List<Automaton> as = new ArrayList<Automaton>();
			while (min-- > 0)
				as.add(a);
			b = concatenate(as);
		}
		if (max > 0) {
			Automaton d = a.clone();
			while (--max > 0) {
				Automaton c = a.clone();
				for (State p : c.getAcceptStates())
					p.addEpsilon(d.initial);
				d = c;
			}
			for (State p : b.getAcceptStates())
				p.addEpsilon(d.initial);
			b.deterministic = false;
			b.clearHashCode();
			b.checkMinimizeAlways();
		}
		return b;
	}

	/**
	 * Returns a (deterministic) automaton that accepts the complement of the
	 * language of the given automaton.
	 * <p>
	 * Complexity: linear in number of states (if already deterministic).
	 */
	static public Automaton complement(Automaton a) {
		a = a.cloneExpandedIfRequired();
		a.determinize();
		a.totalize();
		for (State p : a.getStates())
			p.accept = !p.accept;
		a.removeDeadTransitions();
		return a;
	}

	/**
	 * Returns a (deterministic) automaton that accepts the intersection of
	 * the language of <code>a1</code> and the complement of the language of 
	 * <code>a2</code>. As a side-effect, the automata may be determinized, if not
	 * already deterministic.
	 * <p>
	 * Complexity: quadratic in number of states (if already deterministic).
	 */
	static public Automaton minus(Automaton a1, Automaton a2) {
		if (a1.isEmpty() || a1 == a2)
			return BasicAutomata.makeEmpty();
		if (a2.isEmpty())
			return a1.cloneIfRequired();
		if (a1.isSingleton()) {
			if (a2.run(a1.singleton))
				return BasicAutomata.makeEmpty();
			else
				return a1.cloneIfRequired();
		}
		return intersection(a1, a2.complement());
	}

	/**
	 * Returns an automaton that accepts the intersection of
	 * the languages of the given automata. 
	 * Never modifies the input automata languages.
	 * <p>
	 * Complexity: quadratic in number of states.
	 */
	static public Automaton intersection(Automaton a1, Automaton a2) {
		if (a1.isSingleton()) {
			if (a2.run(a1.singleton))
				return a1.cloneIfRequired();
			else
				return BasicAutomata.makeEmpty();
		}
		if (a2.isSingleton()) {
			if (a1.run(a2.singleton))
				return a2.cloneIfRequired();
			else
				return BasicAutomata.makeEmpty();
		}
		if (a1 == a2)
			return a1.cloneIfRequired();
		Transition[][] transitions1 = Automaton.getSortedTransitions(a1.getStates());
		Transition[][] transitions2 = Automaton.getSortedTransitions(a2.getStates());
		Automaton c = new Automaton();
		LinkedList<StatePair> worklist = new LinkedList<StatePair>();
		HashMap<StatePair, StatePair> newstates = new HashMap<StatePair, StatePair>();
		StatePair p = new StatePair(c.initial, a1.initial, a2.initial);
		worklist.add(p);
		newstates.put(p, p);
		while (worklist.size() > 0) {
			p = worklist.removeFirst();
			p.s.accept = p.s1.accept && p.s2.accept;
			Transition[] t1 = transitions1[p.s1.number];
			Transition[] t2 = transitions2[p.s2.number];
			for (int n1 = 0, b2 = 0; n1 < t1.length; n1++) {
				while (b2 < t2.length && t2[b2].max < t1[n1].min)
					b2++;
				for (int n2 = b2; n2 < t2.length && t1[n1].max >= t2[n2].min; n2++) 
					if (t2[n2].max >= t1[n1].min) {
						StatePair q = new StatePair(t1[n1].to, t2[n2].to);
						StatePair r = newstates.get(q);
						if (r == null) {
							q.s = new State();
							worklist.add(q);
							newstates.put(q, q);
							r = q;
						}
						char min = t1[n1].min > t2[n2].min ? t1[n1].min : t2[n2].min;
						char max = t1[n1].max < t2[n2].max ? t1[n1].max : t2[n2].max;
						p.s.transitions.add(new Transition(min, max, r.s));
					}
			}
		}
		c.deterministic = a1.deterministic && a2.deterministic;
		c.removeDeadTransitions();
		c.checkMinimizeAlways();
		return c;
	}
		
	/**
	 * Returns true if the language of <code>a1</code> is a subset of the
	 * language of <code>a2</code>. 
	 * As a side-effect, <code>a2</code> is determinized if not already marked as
	 * deterministic.
	 * <p>
	 * Complexity: quadratic in number of states.
	 */
	public static boolean subsetOf(Automaton a1, Automaton a2) {
		if (a1 == a2)
			return true;
		if (a1.isSingleton()) {
			if (a2.isSingleton())
				return a1.singleton.equals(a2.singleton);
			return a2.run(a1.singleton);
		}
		a2.determinize();
		Transition[][] transitions1 = Automaton.getSortedTransitions(a1.getStates());
		Transition[][] transitions2 = Automaton.getSortedTransitions(a2.getStates());
		LinkedList<StatePair> worklist = new LinkedList<StatePair>();
		HashSet<StatePair> visited = new HashSet<StatePair>();
		StatePair p = new StatePair(a1.initial, a2.initial);
		worklist.add(p);
		visited.add(p);
		while (worklist.size() > 0) {
			p = worklist.removeFirst();
			if (p.s1.accept && !p.s2.accept)
				return false;
			Transition[] t1 = transitions1[p.s1.number];
			Transition[] t2 = transitions2[p.s2.number];
			for (int n1 = 0, b2 = 0; n1 < t1.length; n1++) {
				while (b2 < t2.length && t2[b2].max < t1[n1].min)
					b2++;
				int min1 = t1[n1].min, max1 = t1[n1].max;
				for (int n2 = b2; n2 < t2.length && t1[n1].max >= t2[n2].min; n2++) {
					if (t2[n2].min > min1)
						return false;
					if (t2[n2].max < Character.MAX_VALUE) 
						min1 = t2[n2].max + 1;
					else {
						min1 = Character.MAX_VALUE;
						max1 = Character.MIN_VALUE;
					}
					StatePair q = new StatePair(t1[n1].to, t2[n2].to);
					if (!visited.contains(q)) {
						worklist.add(q);
						visited.add(q);
					}
				}
				if (min1 <= max1)
					return false;
			}		
		}
		return true;
	}
	
	/**
	 * Returns an automaton that accepts the union of the languages of the given automata.
	 * <p>
	 * Complexity: linear in number of states.
	 */
	public static Automaton union(Automaton a1, Automaton a2) {
		if ((a1.isSingleton() && a2.isSingleton() && a1.singleton.equals(a2.singleton)) || a1 == a2)
			return a1.cloneIfRequired();
		if (a1 == a2) {
			a1 = a1.cloneExpanded();
			a2 = a2.cloneExpanded();
		} else {
			a1 = a1.cloneExpandedIfRequired();
			a2 = a2.cloneExpandedIfRequired();
		}
		State s = new State();
		s.addEpsilon(a1.initial);
		s.addEpsilon(a2.initial);
		a1.initial = s;
		a1.deterministic = false;
		a1.clearHashCode();
		a1.checkMinimizeAlways();
		return a1;
	}
	
	/**
	 * Returns an automaton that accepts the union of the languages of the given automata.
	 * <p>
	 * Complexity: linear in number of states.
	 */
	public static Automaton union(Collection<Automaton> l) {
		Set<Integer> ids = new HashSet<Integer>();
		for (Automaton a : l)
			ids.add(System.identityHashCode(a));
		boolean has_aliases = ids.size() != l.size();
		State s = new State();
		for (Automaton b : l) {
			if (b.isEmpty())
				continue;
			Automaton bb = b;
			if (has_aliases)
				bb = bb.cloneExpanded();
			else
				bb = bb.cloneExpandedIfRequired();
			s.addEpsilon(bb.initial);
		}
		Automaton a = new Automaton();
		a.initial = s;
		a.deterministic = false;
		a.clearHashCode();
		a.checkMinimizeAlways();
		return a;
	}

	/**
	 * Determinizes the given automaton.
	 * <p>
	 * Complexity: exponential in number of states.
	 */
	public static void determinize(Automaton a) {
		if (a.deterministic || a.isSingleton())
			return;
		Set<State> initialset = new HashSet<State>();
		initialset.add(a.initial);
		determinize(a, initialset);
	}

	/** 
	 * Determinizes the given automaton using the given set of initial states. 
	 */
	static void determinize(Automaton a, Set<State> initialset) {
		char[] points = a.getStartPoints();
		// subset construction
		Map<Set<State>, Set<State>> sets = new HashMap<Set<State>, Set<State>>();
		LinkedList<Set<State>> worklist = new LinkedList<Set<State>>();
		Map<Set<State>, State> newstate = new HashMap<Set<State>, State>();
		sets.put(initialset, initialset);
		worklist.add(initialset);
		a.initial = new State();
		newstate.put(initialset, a.initial);
		while (worklist.size() > 0) {
			Set<State> s = worklist.removeFirst();
			State r = newstate.get(s);
			for (State q : s)
				if (q.accept) {
					r.accept = true;
					break;
				}
			for (int n = 0; n < points.length; n++) {
				Set<State> p = new HashSet<State>();
				for (State q : s)
					for (Transition t : q.transitions)
						if (t.min <= points[n] && points[n] <= t.max)
							p.add(t.to);
				if (!sets.containsKey(p)) {
					sets.put(p, p);
					worklist.add(p);
					newstate.put(p, new State());
				}
				State q = newstate.get(p);
				char min = points[n];
				char max;
				if (n + 1 < points.length)
					max = (char)(points[n + 1] - 1);
				else
					max = Character.MAX_VALUE;
				r.transitions.add(new Transition(min, max, q));
			}
		}
		a.deterministic = true;
		a.removeDeadTransitions();
	}

	/** 
	 * Adds epsilon transitions to the given automaton.
	 * This method adds extra character interval transitions that are equivalent to the given
	 * set of epsilon transitions. 
	 * @param pairs collection of {@link StatePair} objects representing pairs of source/destination states 
	 *        where epsilon transitions should be added
	 */
	public static void addEpsilons(Automaton a, Collection<StatePair> pairs) {
		a.expandSingleton();
		HashMap<State, HashSet<State>> forward = new HashMap<State, HashSet<State>>();
		HashMap<State, HashSet<State>> back = new HashMap<State, HashSet<State>>();
		for (StatePair p : pairs) {
			HashSet<State> to = forward.get(p.s1);
			if (to == null) {
				to = new HashSet<State>();
				forward.put(p.s1, to);
			}
			to.add(p.s2);
			HashSet<State> from = back.get(p.s2);
			if (from == null) {
				from = new HashSet<State>();
				back.put(p.s2, from);
			}
			from.add(p.s1);
		}
		// calculate epsilon closure
		LinkedList<StatePair> worklist = new LinkedList<StatePair>(pairs);
		HashSet<StatePair> workset = new HashSet<StatePair>(pairs);
		while (!worklist.isEmpty()) {
			StatePair p = worklist.removeFirst();
			workset.remove(p);
			HashSet<State> to = forward.get(p.s2);
			HashSet<State> from = back.get(p.s1);
			if (to != null) {
				for (State s : to) {
					StatePair pp = new StatePair(p.s1, s);
					if (!pairs.contains(pp)) {
						pairs.add(pp);
						forward.get(p.s1).add(s);
						back.get(s).add(p.s1);
						worklist.add(pp);
						workset.add(pp);
						if (from != null) {
							for (State q : from) {
								StatePair qq = new StatePair(q, p.s1);
								if (!workset.contains(qq)) {
									worklist.add(qq);
									workset.add(qq);
								}
							}
						}
					}
				}
			}
		}
		// add transitions
		for (StatePair p : pairs)
			p.s1.addEpsilon(p.s2);
		a.deterministic = false;
		a.clearHashCode();
		a.checkMinimizeAlways();
	}
	
	/**
	 * Returns true if the given automaton accepts the empty string and nothing else.
	 */
	public static boolean isEmptyString(Automaton a) {
		if (a.isSingleton())
			return a.singleton.length() == 0;
		else
			return a.initial.accept && a.initial.transitions.isEmpty();
	}

	/**
	 * Returns true if the given automaton accepts no strings.
	 */
	public static boolean isEmpty(Automaton a) {
		if (a.isSingleton())
			return false;
		return !a.initial.accept && a.initial.transitions.isEmpty();
	}
	
	/**
	 * Returns true if the given automaton accepts all strings.
	 */
	public static boolean isTotal(Automaton a) {
		if (a.isSingleton())
			return false;
		if (a.initial.accept && a.initial.transitions.size() == 1) {
			Transition t = a.initial.transitions.iterator().next();
			return t.to == a.initial && t.min == Character.MIN_VALUE && t.max == Character.MAX_VALUE;
		}
		return false;
	}
	
	/**
	 * Returns a shortest accepted/rejected string. 
	 * If more than one shortest string is found, the lexicographically first of the shortest strings is returned.
	 * @param accepted if true, look for accepted strings; otherwise, look for rejected strings
	 * @return the string, null if none found
	 */
	public static String getShortestExample(Automaton a, boolean accepted) {
		if (a.isSingleton()) {
			if (accepted)
				return a.singleton;
			else if (a.singleton.length() > 0)
				return "";
			else
				return "\u0000";

		}
		return getShortestExample(a.getInitialState(), accepted);
	}

	static String getShortestExample(State s, boolean accepted) {
		Map<State,String> path = new HashMap<State,String>();
		LinkedList<State> queue = new LinkedList<State>();
		path.put(s, "");
		queue.add(s);
		String best = null;
		while (!queue.isEmpty()) {
			State q = queue.removeFirst();
			String p = path.get(q);
			if (q.accept == accepted) {
				if (best == null || p.length() < best.length() || (p.length() == best.length() && p.compareTo(best) < 0))
					best = p;
			} else 
				for (Transition t : q.getTransitions()) {
					String tp = path.get(t.to);
					String np = p + t.min;
					if (tp == null || (tp.length() == np.length() && np.compareTo(tp) < 0)) {
						if (tp == null)
							queue.addLast(t.to);
						path.put(t.to, np);
					}
				}
		}
		return best;
	}
	
	/**
	 * Returns true if the given string is accepted by the automaton. 
	 * <p>
	 * Complexity: linear in the length of the string.
	 * <p>
	 * <b>Note:</b> for full performance, use the {@link RunAutomaton} class.
	 */
	public static boolean run(Automaton a, String s) {
		if (a.isSingleton()) {
			//System.out.println("Singleton Case");
			return s.equals(a.singleton);
		}
		if (a.deterministic) {
			//System.out.println("Deterministic Case");
			State p = a.initial;
			for (int i = 0; i < s.length(); i++) {
				State q = p.step(s.charAt(i));
				if (q == null)
					return false;
				p = q;
			}
			return p.accept;
		} else {
			Set<State> states = a.getStates();
			Automaton.setStateNumbers(states);
			LinkedList<State> pp = new LinkedList<State>();
			LinkedList<State> pp_other = new LinkedList<State>();
			BitSet bb = new BitSet(states.size());
			BitSet bb_other = new BitSet(states.size());
			pp.add(a.initial);
			ArrayList<State> dest = new ArrayList<State>();
			boolean accept = a.initial.accept;
			for (int i = 0; i < s.length(); i++) {
				//System.out.println("Printing pp =>");
				//printLinkedList(pp);
				char c = s.charAt(i);
				//System.out.println(c);
				accept = false;
				pp_other.clear();
				bb_other.clear();
				for (State p : pp) {
					dest.clear();
					p.step(c, dest);
					for (State q : dest) {
						if (q.accept)
							accept = true;
						if (!bb_other.get(q.number)) {
							bb_other.set(q.number);
							pp_other.add(q);
						}
					}
				}

				LinkedList<State> tp = pp;
				pp = pp_other;
				pp_other = tp;

				BitSet tb = bb;
				bb = bb_other;
				bb_other = tb;
			}
			return accept;
		}
	}

	/**
	 *  Traverses the automaton many times to find matching strings in the input
	 *  otherwise if it doesn't find a matching pattern for a given character or a sequence of characters
	 *  it will return unknown sequence <unknown> where unknown is either a character or a sequence of characters
	 * @param a
	 * @param s
	 * @return
	 */
	public static ArrayList<ArrayList<String>> traverse(Automaton a, String s) {
		ArrayList<ArrayList<String>> tokens = new ArrayList<ArrayList<String>>();

		Set<State> states = a.getStates();
		Automaton.setStateNumbers(states);

		LinkedList<State> pp = new LinkedList<State>();
		LinkedList<State> pp_other = new LinkedList<State>();

		LinkedList<State> dest = new LinkedList<State>();

		BitSet bb = new BitSet(states.size());
		BitSet bb_other = new BitSet(states.size());

		ArrayList<Character> accumulator = new ArrayList<Character>();
		ArrayList<Character> unknowns = new ArrayList<Character>();

		ArrayList<String> indices = new ArrayList<String>();

		Boolean flagUnknowns = false;

		int counterCharTokens = 0;

		pp.add(a.initial);


		int sizeDest = 0;
		boolean flag;
		int i = 0;
		while( i<s.length()) { //
			flag = false;
			sizeDest = 0;
			char c = s.charAt(i);
			System.out.println("i="+i+" c="+c);

			pp_other.clear();
			bb_other.clear();
			for (State p : pp) {
				dest.clear();
				p.step(c, dest);
				sizeDest += dest.size();
				for (State q : dest) {
					if (q.accept) {
						flag = true;
					}
					if (!bb_other.get(q.number)) {
						bb_other.set(q.number);
						pp_other.add(q);
					}
				}
			}

			LinkedList<State> tp = pp;
			pp = pp_other;
			pp_other = tp;

			BitSet tb = bb;
			bb = bb_other;
			bb_other = tb;

			System.out.println("flag="+flag);


			if (flagUnknowns == true) {//End of the unknowns sequence
				flagUnknowns = false;
				String unknownsStr = getStringRepresentation(unknowns);
				if (unknownsStr.length() > 0) {
					ArrayList<String> subsubTokens = new ArrayList<String>();
					subsubTokens.add ("<" + unknownsStr + ">"); //.substring(1,unknownsStr.length()-1)
					counterCharTokens +=unknownsStr.length();
					System.out.println("ADDING 1:"+subsubTokens);
					tokens.add(subsubTokens);
				}
				unknowns.clear();
			}

			/***
			 * Case 1: FINAL STATE REACHED
			 */

			if (flag == true) {
				accumulator.add(c);

				if (i==s.length()-1){ // In case it is the end of the sentence
					String accStr = getStringRepresentation(accumulator);
					if (accStr.length()>0) {
						ArrayList<String> subsubTokens = new ArrayList<String>();
						subsubTokens.add(accStr); //.substring(1,accStr.length()-1)
						indices.add(String.valueOf(i));
						for (int index = 0; index<indices.size();index++) {
							int indexInt = Integer.parseInt(indices.get(index))-counterCharTokens ;
							subsubTokens.add(String.valueOf(indexInt));
						}
						counterCharTokens +=accStr.length() - 1;
						System.out.println("ADDING 2:"+subsubTokens);
						tokens.add(subsubTokens);
						indices.clear();
					}
					accumulator.clear();
					i = i + 1;
				}
				else { // NOT the end of the sentence

					int nextSize = 0;
					char c_next = s.charAt(i+1);
					for (State p_next : pp) {
						dest.clear();
						p_next.step(c_next, dest);
						nextSize += dest.size();
					}


					System.out.println("sizecurrent: "+nextSize);
					if (nextSize==0){
						pp.clear();
						pp.add(a.initial);
						String accStr = getStringRepresentation(accumulator);
						if (accStr.length()>0) {
							ArrayList<String> subsubTokens = new ArrayList<String>();
							subsubTokens.add(accStr); //.substring(1,accStr.length()-1)
							indices.add(String.valueOf(i));
							for (int index = 0; index<indices.size();index++) {
								System.out.println("counterCharTokens:"+counterCharTokens);
								int indexInt = Integer.parseInt(indices.get(index))-counterCharTokens ;
								subsubTokens.add(String.valueOf(indexInt));
							}
							counterCharTokens +=accStr.length() - 1;
							System.out.println("ADDING 3:"+subsubTokens);
							tokens.add(subsubTokens);
							indices.clear();
						}
						accumulator.clear();

					}
					else {
						System.out.println("Storing the index i="+i);
						indices.add(String.valueOf(i));
						i = i + 1;
					}

				}

			}

			/***
			 * Case 2: FINAL STATE NOT REACHED
			 */

			else {
				if (i==s.length()-1 && sizeDest != 0){ // We reached the end we cannot check the next character if it has a transition
					accumulator.add(c);
					String unfinishedPrefix = getStringRepresentation(accumulator);
					if (unfinishedPrefix.length() > 0) {
						ArrayList<String> subsubTokens = new ArrayList<String>();
						System.out.println("accumulator="+accumulator);
						System.out.println("unfinishedPrefix.length()="+unfinishedPrefix.length());
						subsubTokens.add("<" + unfinishedPrefix + ">"); //.substring(1,unfinishedPrefix.length()-1)
						counterCharTokens +=unfinishedPrefix.length();
						System.out.println("ADDING 4:"+subsubTokens);
						tokens.add(subsubTokens);
						counterCharTokens = 0;
					}
				}

				if (i==s.length()-1 && sizeDest == 0 ){
					flagUnknowns = false;
					unknowns.add(c);
					String unknownsStr = getStringRepresentation(unknowns);
					if (unknownsStr.length() > 0) {
						ArrayList<String> subsubTokens = new ArrayList<String>();
						subsubTokens.add ("<" + unknownsStr + ">"); //.substring(1,unknownsStr.length()-1)
						counterCharTokens +=unknownsStr.length();
						System.out.println("ADDING: 5"+subsubTokens);
						tokens.add(subsubTokens);
						//counterCharTokens = 0;

					}
					unknowns.clear();
				}
				else if (i<s.length()-1){
					// Check for the transitions to the next character


					int nextSize = 0;
					char c_next = s.charAt(i+1);
					for (State p_next : pp) {
						dest.clear();
						p_next.step(c_next, dest);
						nextSize += dest.size();
					}


					if ((sizeDest == 0 && nextSize == 0) ||(accumulator.size()==0 && sizeDest != 0 && nextSize == 0)) { // no transition at all
						unknowns.add(c);
						accumulator.clear();
						pp.clear();
						pp_other.clear();
						pp.add(a.initial);
					}
					else if (accumulator.size()>0 && sizeDest != 0 && nextSize == 0) { // no transition at all
						accumulator.add(c);
						String unfinishedPrefix = getStringRepresentation(accumulator);
						if (unfinishedPrefix.length() > 0) {
							ArrayList<String> subsubTokens = new ArrayList<String>();
							subsubTokens.add("<" + unfinishedPrefix + ">"); // .substring(1,unfinishedPrefix.length()-1)
							counterCharTokens +=unfinishedPrefix.length();
							System.out.println("ADDING 6:"+subsubTokens);
							tokens.add(subsubTokens);
							//counterCharTokens = 0;
						}
						accumulator.clear();
						pp.clear();
						pp_other.clear();
						pp.add(a.initial);
						indices.clear();
					}
					else {
						flagUnknowns = true;
						accumulator.add(c);
					}
				}
				i = i + 1;
			}


		}

		return tokens;
	}

	public static class Edge implements Serializable{
		int startIndex;
		int endIndex;
		boolean isKnown;

		public Edge(){

		}

		public Edge(int start, int end, boolean known){
			startIndex = start;
			endIndex = end;
			isKnown = known;
		}

		public int getStartIndex(){
			return startIndex;
		}

		public int getEndIndex(){
			return endIndex;
		}

		public boolean getIsKnown(){
			return isKnown;
		}

		public void setStartIndex(int start){
			startIndex = start;
		}

		public void setEndIndex(int end){
			endIndex = end;
		}

		public void setIsKnown(int isKnown){
			isKnown = isKnown;
		}

		@Override
		public String toString(){
			return this.startIndex+"->"+this.endIndex+" "+this.isKnown;
		}

		@Override
		public boolean equals (Object object) {
			boolean sameSame = false;

			if (object != null && object instanceof BasicOperations.Edge)
			{
				sameSame = ((this.getStartIndex() == ((BasicOperations.Edge) object).getStartIndex()) && (this.getEndIndex() == ((BasicOperations.Edge) object).getEndIndex())&&(this.getIsKnown() == ((BasicOperations.Edge) object).getIsKnown()));
			}

			return sameSame;
		}


	}
	/**
	 * This function traverses the given string and returns the corresponding chart
	 * @param a
	 * @param s
	 * @return
	 */
	//ArrayList<String>
	public static ArrayList<ArrayList<BasicOperations.Edge>> traverseExplicitSolution(Automaton a, String s) {
		ArrayList<ArrayList<BasicOperations.Edge>> chart = new ArrayList<ArrayList<BasicOperations.Edge>>();

		// 1. Create two copies of the string to be tokenized
		String workingCopy = new StringBuilder().append("S").append(s).append("E").toString();

		// 2. Go over the working copy and store the edges of indices delimiters of the tokens
		Set<State> states = a.getStates();
		Automaton.setStateNumbers(states);

		LinkedList<State> pp = new LinkedList<State>();
		LinkedList<State> pp_other = new LinkedList<State>();

		LinkedList<State> dest = new LinkedList<State>();

		BitSet bb = new BitSet(states.size());
		BitSet bb_other = new BitSet(states.size());


		ArrayList<BasicOperations.Edge> indices = new ArrayList<BasicOperations.Edge>();

		pp.add(a.initial);

		int sizeDest = 0;
		boolean flag;
		boolean stuck = false;
		int i = 0;

		int start = 0;

		ArrayList<Integer> shortestPrefix = new ArrayList<Integer>();

		ArrayList<BasicOperations.Edge> bottomChart = new ArrayList<BasicOperations.Edge>();

		while (!workingCopy.equals("SE")) { // Check for the end of tokenization condition
			// Traverse the FSA to find the next states from that character
			System.out.println("Working Copy =====> " + workingCopy);
			flag = false;
			sizeDest = 0;
			char c = workingCopy.charAt(i);

			pp_other.clear();
			bb_other.clear();
			System.out.println("Character " + c);
			for (State p : pp) {
				dest.clear();
				p.step(c, dest);
				sizeDest += dest.size();
				for (State q : dest) {
					if (q.accept) {
						flag = true;
					}
					if (!bb_other.get(q.number)) {
						bb_other.set(q.number);
						pp_other.add(q);
					}
				}
			}

			LinkedList<State> tp = pp;
			pp = pp_other;
			pp_other = tp;

			BitSet tb = bb;
			bb = bb_other;
			bb_other = tb;

			//printLinkedList(pp);

			if (i == workingCopy.length() - 1) {
				//System.out.println("End of Working Copy >>>>");
				//System.out.println("Case FINAL STATE it gets stuck");
				indices.add(new Edge(start, start + i - 1, flag));
				//System.out.println("i= "+i);
				if (shortestPrefix.size() > 0) {
					//System.out.println("shortestPrefix is not empty first index:" + shortestPrefix.get(0));
					workingCopy = new StringBuilder().append("S").append(workingCopy.substring(shortestPrefix.get(0) + 1, workingCopy.length())).toString();
					start = start + shortestPrefix.get(0); // Update start index
					shortestPrefix.clear();
				} else {
					//System.out.println("shortestPrefix is EMPTY");

					workingCopy = new StringBuilder().append("S").append(workingCopy.substring(i, workingCopy.length())).toString();
					start = start + i - 1; // Update start index
				}

				i = 0;
				pp.clear();
				pp.add(a.initial);
				if (stuck == false) {
					bottomChart.add(indices.get(indices.size() - 1));
				}
				stuck = false;

			} else {
				if (flag == true) {
					int nextSize = 0;

					char c_next = workingCopy.charAt(i + 1);
					for (State p_next : pp) {
						dest.clear();
						p_next.step(c_next, dest);
						nextSize += dest.size();
					}
					indices.add(new Edge(start, start + i - 1, flag));
					if (nextSize == 0) { // If it gets stuck
						//System.out.println("Case FINAL STATE it gets stuck");
						//System.out.println("i= "+i);
						if (shortestPrefix.size() > 0) {
							//System.out.println("shortestPrefix is not empty first index:"+shortestPrefix.get(0));
							workingCopy = new StringBuilder().append("S").append(workingCopy.substring(shortestPrefix.get(0) + 1, workingCopy.length())).toString();
							start = start + shortestPrefix.get(0); // Update start index
							shortestPrefix.clear();
						} else {
							//System.out.println("shortestPrefix is EMPTY");
							workingCopy = new StringBuilder().append("S").append(workingCopy.substring(i, workingCopy.length())).toString();
							start = start + i - 1; // Update start index
						}

						i = 0;
						pp.clear();
						pp.add(a.initial);
						if (stuck == false) {
							bottomChart.add(indices.get(indices.size() - 1));
						}
						stuck = false;

					} else {
						//System.out.println("Adding to Shortest Prefix>>>>>>");
						shortestPrefix.add(i - 1);
						//System.out.println("Case FINAL STATE it doesn't get stuck: "+i);
						//If it doesn't get stuck
						i = i + 1;
						if (stuck == false) {
							bottomChart.add(indices.get(indices.size() - 1));
						}
						stuck = true;
					}

				}
				//System.out.println("Case NOT");
				else {
					i = i + 1;
				}
			}
		}


		chart.add(bottomChart);

		for (int k=1;k<bottomChart.size();k++){
			ArrayList<BasicOperations.Edge> partChart = new ArrayList<BasicOperations.Edge>();
			for (int l=0;l<bottomChart.size()-k;l++) {
				int startIndex = bottomChart.get(l).getStartIndex();
				int endIndex = bottomChart.get(l+k).getEndIndex();
				BasicOperations.Edge edge = new BasicOperations.Edge(startIndex,endIndex,true);
				if (indices.contains(edge)){
					partChart.add(edge);
				}
				else {
					partChart.add(null);
				}

			}
			chart.add(partChart);
		}

		return chart;
	}

	public static ArrayList<ArrayList<ArrayList<BasicOperations.Edge>>> traverseShortSolution(Automaton tokFSA, Automaton sepFSA, HashMap<String,SepSpecification> specifications, String str) {

		///************************* 1. Initialization Phase ************************///
		/**
		 * Output: list of 2 dimensional array charts
		 */

		ArrayList<ArrayList<ArrayList<BasicOperations.Edge>>> charts = new ArrayList<ArrayList<ArrayList<BasicOperations.Edge>>>();
		Set<Integer> shortestPrefix = new HashSet<Integer>();
		ArrayList<BasicOperations.Edge> arcs = new ArrayList<BasicOperations.Edge>();
		ArrayList<BasicOperations.Edge> bottomChart = new ArrayList<BasicOperations.Edge>();

		/**
		 * 1.1. Initializing Variables for traversal with FSA of tokens
		 */
		Set<State> tokStates = tokFSA.getStates();
		Automaton.setStateNumbers(tokStates);
		LinkedList<State> pp_tok = new LinkedList<State>();
		LinkedList<State> dest_tok = new LinkedList<State>();
		LinkedList<State> pp_other_tok = new LinkedList<State>();
		BitSet bb_tok = new BitSet(tokStates.size());
		BitSet bb_other_tok = new BitSet(tokStates.size());
		pp_tok.add(tokFSA.initial);

		/**
		 * 1.2. Initializing Variables for traversal with FSA of separators
		 */
		Set<State> sepStates = sepFSA.getStates();
		Automaton.setStateNumbers(sepStates);
		LinkedList<State> pp_sep = new LinkedList<State>();
		LinkedList<State> dest_sep = new LinkedList<State>();
		LinkedList<State> pp_other_sep = new LinkedList<State>();
		BitSet bb_sep = new BitSet(sepStates.size());
		BitSet bb_other_sep = new BitSet(sepStates.size());
		pp_sep.add(sepFSA.initial);

		/**
		 * Position Pointer to be updated as we traverse the string str
		 */
		int i = 0;
		int start = 0;

		/**
		 * Variables to check whether there is a transition for that currently traversed character using each FSA
		 */
		int sizeDestTok, sizeDestSep;
		boolean accept_tok, accept_sep;
		boolean bottomChartFlag = false;
		boolean flag_sep = false;

		///************************* 2. Traversal Phase ************************///

		while (i < str.length()) {

			char c = str.charAt(i);
			System.out.println("CHARACTER:::::::" + c);
			/**
			 * 2.1. Trying with tokFSA
			 */
			sizeDestTok = sizeDestSep = 0;
			accept_tok = accept_sep = false;

			pp_other_tok.clear();
			bb_other_tok.clear();
			for (State p : pp_tok) {
				dest_tok.clear();
				p.step(c, dest_tok);
				sizeDestTok += dest_tok.size();
				for (State q : dest_tok) {
					if (q.accept) {
						accept_tok = true;
					}
					if (!bb_other_tok.get(q.number)) {
						bb_other_tok.set(q.number);
						pp_other_tok.add(q);
					}
				}
			}

			LinkedList<State> tp_tok = pp_tok;
			pp_tok = pp_other_tok;
			pp_other_tok = tp_tok;

			BitSet tb_tok = bb_tok;
			bb_tok = bb_other_tok;
			bb_other_tok = tb_tok;


			if (sizeDestTok != 0 && flag_sep == false) {
				System.out.println("POSSIBLE TOKEN");

				Automaton.setStateNumbers(sepStates);
				pp_sep = new LinkedList<State>();
				dest_sep = new LinkedList<State>();
				pp_other_sep = new LinkedList<State>();
				bb_sep = new BitSet(sepStates.size());
				bb_other_sep = new BitSet(sepStates.size());
				pp_sep.clear();
				pp_sep.add(sepFSA.initial);

				if (accept_tok == true) {
					System.out.println("ACCEPT STATE TOKEN");
					if (i == str.length() - 1) {
						//System.out.println("ACCEPT"+" i="+i+" shortestPrefix.size()="+shortestPrefix.size());
						if (bottomChartFlag == false) {
							bottomChart.add(new Edge(start, i + 1, true));
						}
						arcs.add(new Edge(start, i + 1, true));
						//System.out.println("END");
						if (shortestPrefix.size() > 0) {
							bottomChartFlag = false;
							//System.out.println("NOT EMPTY"+shortestPrefix);
							i = start = shortestPrefix.iterator().next();
							shortestPrefix.remove(shortestPrefix.iterator().next());
						} else {
							i = i + 1;
						}

					} else {
						c = str.charAt(i + 1);
						int sizeDestSeparator = 0;
						for (State p : pp_sep) {
							dest_sep.clear();
							p.step(c, dest_sep);
							sizeDestSeparator += dest_sep.size();
						}
						if (sizeDestSeparator > 0) {

							// Found a separator so the token is possible
							if (bottomChartFlag == false) {
								System.out.println("Adding EDGE HERE");
								bottomChart.add(new Edge(start, i + 1, true));
							}
							arcs.add(new Edge(start, i + 1, true));
							/*
							bottomChart.add(new Edge(i+1, i + 2, true));
							arcs.add(new Edge(i+1, i + 2, true));
							shortestPrefix.add(i + 1);
							*/
							/**
							 * Check if it gets stuck here
							 */

							int sizeDest = 0;
							c = str.charAt(i + 1);


							for (State p : pp_tok) {
								dest_tok.clear();
								p.step(c, dest_tok);
								//printLinkedList(dest_tok);
								sizeDest += dest_tok.size();
							}

							if (sizeDest == 0) {
								System.out.println("It gets stuck");
								/**
								 * Take the end of the shortest arc used to update
								 */
								if (bottomChartFlag == true) {
									System.out.println("Adding EDGE HERE");
									bottomChart.add(new Edge(i-1, i + 1, true));
									arcs.add(new Edge(start, i + 1, true));
								}
								bottomChartFlag = false;
								Automaton.setStateNumbers(tokStates);
								pp_tok = new LinkedList<State>();
								dest_tok = new LinkedList<State>();
								pp_other_tok = new LinkedList<State>();
								bb_tok = new BitSet(tokStates.size());
								bb_other_tok = new BitSet(tokStates.size());
								pp_tok.clear();
								pp_tok.add(tokFSA.initial);

								//System.out.println(shortestPrefix);
								if (shortestPrefix.size() > 0) {
									System.out.println("shortestPrefix is NOT empty");
									//System.out.println(shortestPrefix);
									if (i >= shortestPrefix.iterator().next()) {
										i = start = shortestPrefix.iterator().next();
										//System.out.println(start);
										shortestPrefix.remove(shortestPrefix.iterator().next());
									} else {
										i = i + 1;
										start = i;
									}
								} else {
									//System.out.println("shortestPrefix is empty");
									i = i + 1;
									start = i;
								}
							} else {
								if (i == str.length() - 2) {
									bottomChartFlag = false;
									Automaton.setStateNumbers(tokStates);
									pp_tok = new LinkedList<State>();
									dest_tok = new LinkedList<State>();
									pp_other_tok = new LinkedList<State>();
									bb_tok = new BitSet(tokStates.size());
									bb_other_tok = new BitSet(tokStates.size());
									pp_tok.clear();
									pp_tok.add(tokFSA.initial);
									i = i + 1;
									start = i;

								} else {
									//System.out.println(i);
									//System.out.println(str.length());
									//System.out.println("It doesn't get stuck");
									bottomChartFlag = true;
									shortestPrefix.add(i + 1);
									i = i + 1;
								}
							}
						} else {
							i = i + 1;
						}
					}
				} else {
					//System.out.println(shortestPrefix);

					// CHECK IF THERE IS A TRANSITION TO THE NEXT CHARACTER
					// IF THERE ISN'T ANY BEFORE REACHING THEN SWITCH TO SEPFSA
					if (i == str.length() - 1 ){//&& sepFSA.run(str.substring(start,i+1))){

						if (shortestPrefix.contains(i)){
							shortestPrefix.remove(i);
						}
						if (shortestPrefix.size() > 0) {
							//System.out.println("shortestPrefix is NOT empty");
							//System.out.println(shortestPrefix);
							if (i >= shortestPrefix.iterator().next()) {
								i = start = shortestPrefix.iterator().next();
								//System.out.println(start);
								shortestPrefix.remove(shortestPrefix.iterator().next());
							} else {
								flag_sep = true;
								i = start;
							}
						} else {
							//System.out.println("shortestPrefix is empty");
							flag_sep = true;
							i = start;
						}
					} else {
						/**
						 * Check if it gets stuck here
						 */
						int sizeDest = 0;
						char c_next = str.charAt(i + 1);


						if (shortestPrefix.contains(i)){
							shortestPrefix.remove(i);
						}
						for (State p : pp_tok) {
							dest_tok.clear();
							p.step(c_next, dest_tok);
							//printLinkedList(dest_tok);
							sizeDest += dest_tok.size();
						}
						if (sizeDest == 0) {
							//System.out.println("No possible TRANSITION FINAL STATE IS FALSE");

							if (shortestPrefix.size() > 0) {
								//System.out.println("shortestPrefix is NOT empty");
								//System.out.println(shortestPrefix);
								if (i >= shortestPrefix.iterator().next()) {
									i = start = shortestPrefix.iterator().next();
									//System.out.println(start);
									shortestPrefix.remove(shortestPrefix.iterator().next());
								} else {
									flag_sep = true;
									i = start;
								}
							} else {
								//System.out.println("shortestPrefix is empty");
								flag_sep = true;
								i = start;
							}
							flag_sep = true;
							bottomChartFlag = false;
							Automaton.setStateNumbers(tokStates);
							pp_tok = new LinkedList<State>();
							dest_tok = new LinkedList<State>();
							pp_other_tok = new LinkedList<State>();
							bb_tok = new BitSet(tokStates.size());
							bb_other_tok = new BitSet(tokStates.size());
							pp_tok.clear();
							pp_tok.add(tokFSA.initial);

						} else {
							i = i + 1;
						}
					}

				}

			} else {
				bottomChartFlag = false;
				Automaton.setStateNumbers(tokStates);
				pp_tok = new LinkedList<State>();
				dest_tok = new LinkedList<State>();
				pp_other_tok = new LinkedList<State>();
				bb_tok = new BitSet(tokStates.size());
				bb_other_tok = new BitSet(tokStates.size());
				pp_tok.clear();
				pp_tok.add(tokFSA.initial);
				/**
				 * 2.1. Trying with sepFSA
				 */
				pp_other_sep.clear();
				bb_other_sep.clear();
				for (State p : pp_sep) {
					dest_sep.clear();
					p.step(c, dest_sep);
					sizeDestSep += dest_sep.size();
					for (State q : dest_sep) {
						if (q.accept) {
							accept_sep = true;
						}
						if (!bb_other_sep.get(q.number)) {
							bb_other_sep.set(q.number);
							pp_other_sep.add(q);
						}
					}
				}

				LinkedList<State> tp_sep = pp_sep;
				pp_sep = pp_other_sep;
				pp_other_sep = tp_sep;

				BitSet tb_sep = bb_sep;
				bb_sep = bb_other_sep;
				bb_other_sep = tb_sep;


				if (sizeDestSep != 0) {
					System.out.println("POSSIBLE SEP");
					/**
					 * CASE 2: Separator Input
					 */

					if (accept_sep == true) {

						System.out.println("ACCEPT >>>> " + c);

						if (i == str.length() - 1) {
							if (specifications.get(str.substring(i,i+1)).getPersistent()) {
								bottomChart.add(new Edge(start, i + 1, true));
								arcs.add(new Edge(start, i + 1, true));
							}
							//System.out.println("Gets stuck at " + c + "edge=> " + str.substring(start, i + 1));

							String key = new StringBuilder().append(c).toString();
							//System.out.println("Checking to build chart at key=>" + key);
							if (specifications.get(key).getEOS()) {
								//System.out.println("BUILDING CHART");
								ArrayList<ArrayList<BasicOperations.Edge>> chart = new ArrayList<ArrayList<BasicOperations.Edge>>();
								ArrayList<BasicOperations.Edge> bottomChartCopy = bottomChart;
								chart.add(bottomChartCopy);

								for (int k = 1; k < bottomChart.size(); k++) {
									ArrayList<BasicOperations.Edge> partChart = new ArrayList<BasicOperations.Edge>();
									for (int l = 0; l < bottomChart.size() - k; l++) {
										int startIndex = bottomChart.get(l).getStartIndex();
										int endIndex = bottomChart.get(l + k).getEndIndex();
										BasicOperations.Edge edge = new BasicOperations.Edge(startIndex, endIndex, true);
										if (arcs.contains(edge)) {
											partChart.add(edge);
										} else {
											partChart.add(null);
										}

									}
									chart.add(partChart);
								}
								charts.add(chart);
							}
							i = i + 1;

						} else {
							/**
							 * Check if it gets stuck here
							 */
							int sizeDest = 0;
							char c_next = str.charAt(i + 1);

							for (State p : pp_sep) {
								dest_sep.clear();
								p.step(c_next, dest_sep);
								sizeDest += dest_sep.size();
							}
							if (sizeDest == 0) {
								flag_sep = false;
								/*
								if (sepFSA.run(str.substring(start,i+1))){
									//System.out.println("BOTTOMCHARTFLAG=="+bottomChartFlag);
									if (bottomChartFlag == false && specifications.get(str.substring(i,i+1)).getPersistent()) {
										//System.out.println("ADDING EDGE>>> "+ start +" -> "+ jjjj);
										//System.out.println("RUNNED>>> "+start);
										bottomChart.add(new Edge(start, i + 1, true));
									}
									if (specifications.get(str.substring(i, i + 1)).getPersistent()) {
										arcs.add(new Edge(start, i + 1, true));
									}
								}
								else {*/
								if (specifications.get(str.substring(i, i + 1)).getPersistent()) {
									bottomChart.add(new Edge(start, i + 1, true));
									arcs.add(new Edge(start, i + 1, true));
								}
								//System.out.println("Gets stuck at " + c);
								/**
								 * Take the end of the shortest arc used to update the pointer
								 */
								bottomChartFlag = false;
								Automaton.setStateNumbers(sepStates);
								pp_sep = new LinkedList<State>();
								dest_sep = new LinkedList<State>();
								pp_other_sep = new LinkedList<State>();
								bb_sep = new BitSet(sepStates.size());
								bb_other_sep = new BitSet(sepStates.size());
								pp_sep.clear();
								pp_sep.add(sepFSA.initial);

								//System.out.println("CHECKING TO BUILD CHART WITH CHARACTER => "+c);

								/**
								 * Checking to build Chart
								 */

								//System.out.println("RIGHT");
								String key = new StringBuilder().append(c).toString();
								//if (i<str.length()-1){
								//if (!sepFSA.run(str.substring(i+1,i+2)) || str.substring(i+1,i+2).equals(" ")) {
								if (specifications.get(key).getEOS()) {
									int startIndex = arcs.get(arcs.size() - 1).getStartIndex();
									int endIndex = arcs.get(arcs.size() - 1).getEndIndex();
									boolean buildChart = true;
									for (int indicesArcs = 0; indicesArcs < arcs.size(); indicesArcs++) {
										if (arcs.get(indicesArcs).getStartIndex() <= startIndex && endIndex < arcs.get(indicesArcs).getEndIndex() && arcs.get(indicesArcs).getIsKnown() == true) {
											buildChart = false;
										}
									}
									//System.out.println("CHARACTER IS==>"+i);
									/*
									if (i <str.length() - 2) {
										c_next = str.charAt(i + 1);
										char c_next_next = str.charAt(i+2);
										//System.out.println("c_next= "+c_next+" c_next_next= "+c_next_next);
										if ((c_next != ' ' && c_next!='\"' && c_next!='\'' && c_next!=')' && c_next!='(' && c_next!=']' && c_next!='[' && c_next!='}' && c_next!='{'  )|| c_next_next == '-'  ||  Character.isLowerCase(c_next_next) || c_next_next == '\'' || c_next_next == ',' || c_next_next == '”' || Character.isDigit(c_next_next)) {
											//System.out.println("FALSE");
											buildChart = false;
										}
									}*/
									/*
									if (i == str.length() - 2){
										buildChart = false;
									}*/
									if (buildChart == true) {
										//System.out.println("BUILDING CHART");
										ArrayList<ArrayList<BasicOperations.Edge>> chart = new ArrayList<ArrayList<BasicOperations.Edge>>();
										ArrayList<BasicOperations.Edge> bottomChartCopy = bottomChart;
										chart.add(bottomChartCopy);


										for (int k = 1; k < bottomChart.size(); k++) {
											ArrayList<BasicOperations.Edge> partChart = new ArrayList<BasicOperations.Edge>();
											for (int l = 0; l < bottomChart.size() - k; l++) {
												startIndex = bottomChart.get(l).getStartIndex();
												endIndex = bottomChart.get(l + k).getEndIndex();
												BasicOperations.Edge edge = new BasicOperations.Edge(startIndex, endIndex, true);
												if (arcs.contains(edge)) {
													partChart.add(edge);
												} else {
													partChart.add(null);
												}

											}
											chart.add(partChart);
										}
										charts.add(chart);
										arcs = new ArrayList<BasicOperations.Edge>();
										bottomChart = new ArrayList<BasicOperations.Edge>();

									}

								}
								start = i = i + 1;
							}else {
								bottomChartFlag = true;
								i = i + 1;
							}
						}
					} else {
						i = i + 1;
					}
				} else {
					/**
					 * CASE 3: Unknown Input
					 */

					//System.out.println("UNKNOWN INPUT>>>"+c);
					bottomChart.add(new Edge(start, i + 1, false));
					start = i = i + 1;

				}
			}
		}
		return charts;
	}

	public static ArrayList<ArrayList<ArrayList<BasicOperations.Edge>>> traverseExtendedSolution(Automaton tokFSA, Automaton sepFSA, HashMap<String,SepSpecification> specifications, String str){

		///************************* 1. Initialization Phase ************************///
		/**
		 * Output: list of 2 dimensional array charts
		 */

		int counter = 0;
		ArrayList<ArrayList<ArrayList<BasicOperations.Edge>>> charts = new ArrayList<ArrayList<ArrayList<BasicOperations.Edge>>>();
		Set<Integer> shortestPrefix = new HashSet<Integer>();
		ArrayList<BasicOperations.Edge> arcs = new ArrayList<BasicOperations.Edge>();
		ArrayList<BasicOperations.Edge> bottomChart = new ArrayList<BasicOperations.Edge>();

		/**
		 * 1.1. Initializing Variables for traversal with FSA of tokens
		 */
		Set<State> tokStates = tokFSA.getStates();
		Automaton.setStateNumbers(tokStates);
		LinkedList<State> pp_tok = new LinkedList<State>();
		LinkedList<State> dest_tok = new LinkedList<State>();
		LinkedList<State> pp_other_tok = new LinkedList<State>();
		BitSet bb_tok = new BitSet(tokStates.size());
		BitSet bb_other_tok = new BitSet(tokStates.size());
		pp_tok.add(tokFSA.initial);

		/**
		 * 1.2. Initializing Variables for traversal with FSA of separators
		 */
		Set<State> sepStates = sepFSA.getStates();
		Automaton.setStateNumbers(sepStates);
		LinkedList<State> pp_sep = new LinkedList<State>();
		LinkedList<State> dest_sep = new LinkedList<State>();
		LinkedList<State> pp_other_sep = new LinkedList<State>();
		BitSet bb_sep = new BitSet(sepStates.size());
		BitSet bb_other_sep = new BitSet(sepStates.size());
		pp_sep.add(sepFSA.initial);

		/**
		 * Position Pointer to be updated as we traverse the string str
		 */
		int i= 0;
		int start = 0;

		/**
		 * Variables to check whether there is a transition for that currently traversed character using each FSA
 		 */
		int sizeDestTok,sizeDestSep;
		boolean accept_tok,accept_sep;
		boolean bottomChartFlag = false;
		boolean flag_sep = false;

		///************************* 2. Traversal Phase ************************///

		while(i<str.length()) {
			/*
			for (int o=0;o<arcs.size();o++){
				System.out.println(arcs.get(o));
			}
			*/
			char c = str.charAt(i);
			//System.out.println("CHARACTER:::::::"+c);
			/**
			 * 2.1. Trying with tokFSA
			 */
			sizeDestTok = sizeDestSep = 0;
			accept_tok = accept_sep = false;

			pp_other_tok.clear();
			bb_other_tok.clear();
			for (State p : pp_tok) {
				dest_tok.clear();
				p.step(c, dest_tok);
				sizeDestTok += dest_tok.size();
				for (State q : dest_tok) {
					if (q.accept) {
						accept_tok = true;
					}
					if (!bb_other_tok.get(q.number)) {
						bb_other_tok.set(q.number);
						pp_other_tok.add(q);
					}
				}
			}

			LinkedList<State> tp_tok = pp_tok;
			pp_tok = pp_other_tok;
			pp_other_tok = tp_tok;

			BitSet tb_tok = bb_tok;
			bb_tok = bb_other_tok ;
			bb_other_tok  = tb_tok ;

			if (i<str.length()-1) {
				if (c == '-' && Character.isAlphabetic(str.charAt(i + 1)) && start==i) {
					sizeDestTok = 0; // Consider it as a separator
				}
			}

			if(sizeDestTok!=0 && flag_sep==false){
				//System.out.println("POSSIBLE TOKEN");

				Automaton.setStateNumbers(sepStates);
				pp_sep = new LinkedList<State>();
				dest_sep = new LinkedList<State>();
				pp_other_sep = new LinkedList<State>();
				bb_sep = new BitSet(sepStates.size());
				bb_other_sep = new BitSet(sepStates.size());
				pp_sep.clear();
				pp_sep.add(sepFSA.initial);

				if (accept_tok == true) {
					if (i == str.length() - 1) {
						//System.out.println("ACCEPT"+" i="+i+" shortestPrefix.size()="+shortestPrefix.size());
						if (bottomChartFlag == false) {
							bottomChart.add(new Edge(start, i + 1, true));
						}
						arcs.add(new Edge(start, i + 1, true));
						//System.out.println("END");
						if (shortestPrefix.size()>0) {
							bottomChartFlag = false;
							//System.out.println("NOT EMPTY"+shortestPrefix);
							i = start = shortestPrefix.iterator().next();
							shortestPrefix.remove(shortestPrefix.iterator().next());
						}
						else {
							//System.out.println("BUILDING CHART");
							i  = i + 1;

							/*
							for (int o=0;o<arcs.size();o++){
								System.out.println(arcs.get(o));
							}
							*/
							// BUILD CHART
							ArrayList<ArrayList<BasicOperations.Edge>> chart = new ArrayList<ArrayList<BasicOperations.Edge>>();
							ArrayList<BasicOperations.Edge> bottomChartCopy = bottomChart;
							chart.add(bottomChartCopy);

							for (int k = 1; k < bottomChart.size(); k++) {
								ArrayList<BasicOperations.Edge> partChart = new ArrayList<BasicOperations.Edge>();
								for (int l = 0; l < bottomChart.size() - k; l++) {
									int startIndex = bottomChart.get(l).getStartIndex();
									int endIndex = bottomChart.get(l + k).getEndIndex();
									BasicOperations.Edge edge = new BasicOperations.Edge(startIndex, endIndex, true);
									if (arcs.contains(edge)) {
										partChart.add(edge);
									} else {
										partChart.add(null);
									}

								}
								chart.add(partChart);
							}
							charts.add(chart);
						}

					} else {
						c = str.charAt(i + 1);
						int sizeDestSeparator = 0;
						for (State p : pp_sep) {
							dest_sep.clear();
							p.step(c, dest_sep);
							sizeDestSeparator += dest_sep.size();
						}

						int difference = i + 1 - start;
						String token = str.substring(start,i+1);
						String [] problematicsArr = {"co","Co","Prof","prof","p.m","Ga","ga","Inc","Corp","Ila",
								"Jr","JR","jr","Sr","SR","sr","Ill","Colo","colo","miss","Miss","Mr","Calif",
								"calif","N.Y","St","st","Minn","Pa","pa","Md","md","N.J","Ore","La","Nev","Mrs"};
						List<String> problematics = Arrays.asList(problematicsArr);
						if ((i!=str.length()-2) && (sizeDestSeparator == 0 || (c=='.' && difference==1) || (problematics.contains(token)) )){
								i = i + 1;
						} else {
							// Found a separator so the token is possible
							if (bottomChartFlag == false) {
								bottomChart.add(new Edge(start, i + 1, true));
							}
							arcs.add(new Edge(start, i + 1, true));

							/**
							 * Check if it gets stuck here
							 */

							int sizeDest = 0;
							c = str.charAt(i + 1);


							for (State p : pp_tok) {
								dest_tok.clear();
								p.step(c, dest_tok);
								//printLinkedList(dest_tok);
								sizeDest += dest_tok.size();
							}
							if (i<str.length()-3){
								if (Character.isDigit(str.charAt(i)) && (str.charAt(i + 1)==',' ||str.charAt(i + 1) =='-'||str.charAt(i + 1)== '.' || str.charAt(i + 1)==';' ||str.charAt(i + 1) == '/') && Character.isLetter(str.charAt(i + 2))){
									sizeDest = 0;
								}
							}

							if (sizeDest == 0) {
								//System.out.println("It gets stuck");
								/**
								 * Take the end of the shortest arc used to update
								 */
								bottomChartFlag = false;
								Automaton.setStateNumbers(tokStates);
								pp_tok = new LinkedList<State>();
								dest_tok = new LinkedList<State>();
								pp_other_tok = new LinkedList<State>();
								bb_tok = new BitSet(tokStates.size());
								bb_other_tok = new BitSet(tokStates.size());
								pp_tok.clear();
								pp_tok.add(tokFSA.initial);

								if (shortestPrefix.size() > 0) {
									//System.out.println("shortestPrefix is NOT empty");
									//System.out.println(shortestPrefix);
									if (i >= shortestPrefix.iterator().next()) {
										i = start = shortestPrefix.iterator().next();
										//System.out.println(start);
										shortestPrefix.remove(shortestPrefix.iterator().next());
									} else {
										i = i + 1;
										start = i;
									}
								} else {
									//System.out.println("shortestPrefix is empty");
									i = i + 1;
									start = i;
								}
							} else {
								if (i == str.length() - 2) {
									bottomChartFlag = false;
									Automaton.setStateNumbers(tokStates);
									pp_tok = new LinkedList<State>();
									dest_tok = new LinkedList<State>();
									pp_other_tok = new LinkedList<State>();
									bb_tok = new BitSet(tokStates.size());
									bb_other_tok = new BitSet(tokStates.size());
									pp_tok.clear();
									pp_tok.add(tokFSA.initial);
									i = i + 1;
									start = i;

								} else {
									//System.out.println(i);
									//System.out.println(str.length());
									//System.out.println("It doesn't get stuck");
									bottomChartFlag = true;
									shortestPrefix.add(i + 1);
									i = i + 1;
								}
							}
						}
					}
				}
				else{
					if (i == str.length() - 1 && c!='.' && c!='…' && c!='?' && c!='!' && c!=';' && c!='|' && c!=']') {
						ArrayList<ArrayList<BasicOperations.Edge>> chart = new ArrayList<ArrayList<BasicOperations.Edge>>();
						ArrayList<BasicOperations.Edge> bottomChartCopy = bottomChart;
						chart.add(bottomChartCopy);

						for (int k = 1; k < bottomChart.size(); k++) {
							ArrayList<BasicOperations.Edge> partChart = new ArrayList<BasicOperations.Edge>();
							for (int l = 0; l < bottomChart.size() - k; l++) {
								int startIndex = bottomChart.get(l).getStartIndex();
								int endIndex = bottomChart.get(l + k).getEndIndex();
								BasicOperations.Edge edge = new BasicOperations.Edge(startIndex, endIndex, true);
								if (arcs.contains(edge)) {
									partChart.add(edge);
								} else {
									partChart.add(null);
								}

							}
							chart.add(partChart);
						}
						charts.add(chart);
					}
					// IF ONE OF THE TERMINATOR SEPARATORS, THEN CHECK NEXT TRANSITION
					if (c=='.' || c=='…' || c=='?' || c=='!' || c==';' || c=='|' || c==']') {
						/**
						 * Check if it gets stuck here
						 */
						if (i == str.length() - 1){
							flag_sep = true;
						} else {
							int sizeDest = 0;
							char c_next = str.charAt(i + 1);


							for (State p : pp_tok) {
								dest_tok.clear();
								p.step(c_next, dest_tok);
								//printLinkedList(dest_tok);
								sizeDest += dest_tok.size();
							}
							if (sizeDest == 0) {
								flag_sep = true;
							} else {
								i = i + 1;
							}
						}
					} else {
						i = i + 1;
					}
				}

			} else{
				flag_sep=false;
				Automaton.setStateNumbers(tokStates);
				pp_tok = new LinkedList<State>();
				dest_tok = new LinkedList<State>();
				pp_other_tok = new LinkedList<State>();
				bb_tok = new BitSet(tokStates.size());
				bb_other_tok = new BitSet(tokStates.size());
				pp_tok.clear();
				pp_tok.add(tokFSA.initial);
				/**
				 * 2.1. Trying with sepFSA
				 */
				pp_other_sep.clear();
				bb_other_sep.clear();
				for (State p : pp_sep) {
					dest_sep.clear();
					p.step(c, dest_sep);
					sizeDestSep += dest_sep.size();
					for (State q : dest_sep) {
						if (q.accept) {
							accept_sep = true;
						}
						if (!bb_other_sep.get(q.number)) {
							bb_other_sep.set(q.number);
							pp_other_sep.add(q);
						}
					}
				}

				LinkedList<State> tp_sep = pp_sep;
				pp_sep = pp_other_sep;
				pp_other_sep = tp_sep;

				BitSet tb_sep = bb_sep;
				bb_sep = bb_other_sep;
				bb_other_sep  = tb_sep ;


				if (sizeDestSep!=0){
					//System.out.println("POSSIBLE SEP");
					/**
					 * CASE 2: Separator Input
					 */

					if (accept_sep == true) {

						//System.out.println("ACCEPT >>>> "+c);

						if (sepFSA.run(str.substring(start,i+1))){
							//System.out.println("BOTTOMCHARTFLAG=="+bottomChartFlag);
							if (bottomChartFlag == false && specifications.get(str.substring(i,i+1)).getPersistent()) {
								int jjjj= i + 1;
								//System.out.println("ADDING EDGE>>> "+ start +" -> "+ jjjj);
								//System.out.println("RUNNED>>> "+start);
								bottomChart.add(new Edge(start, i + 1, true));
							}
							if (specifications.get(str.substring(i, i + 1)).getPersistent()) {
								arcs.add(new Edge(start, i + 1, true));
							}
						}
						else {
							if (bottomChartFlag == false) {
								bottomChart.add(new Edge(start, i + 1, true));
							}
							arcs.add(new Edge(start, i + 1, true));
						}

						if (i == str.length() - 1) {
							//System.out.println("Building CHARTS");
							//System.out.println("END>>> "+start);
							ArrayList<ArrayList<BasicOperations.Edge>> chart = new ArrayList<ArrayList<BasicOperations.Edge>>();
							ArrayList<BasicOperations.Edge> bottomChartCopy = bottomChart;
							chart.add(bottomChartCopy);

							for (int k = 1; k < bottomChart.size(); k++) {
								ArrayList<BasicOperations.Edge> partChart = new ArrayList<BasicOperations.Edge>();
								for (int l = 0; l < bottomChart.size() - k; l++) {
									int startIndex = bottomChart.get(l).getStartIndex();
									int endIndex = bottomChart.get(l + k).getEndIndex();
									BasicOperations.Edge edge = new BasicOperations.Edge(startIndex, endIndex, true);
									if (arcs.contains(edge)) {
										partChart.add(edge);
									} else {
										partChart.add(null);
									}

								}
								chart.add(partChart);
							}
							charts.add(chart);
							i = i + 1;

						} else {
							//System.out.println("TIMBO");
							/**
							 * Check if it gets stuck here
							 */
							int sizeDest = 0;
							char c_next = str.charAt(i + 1);

							for (State p : pp_sep) {
								dest_sep.clear();
								p.step(c_next, dest_sep);
								sizeDest += dest_sep.size();
							}
							if (sizeDest == 0) {
								//System.out.println("Gets stuck");
								/**
								 * Take the end of the shortest arc used to update
								 */
								int stuck = i +1;
								bottomChartFlag = false;
								Automaton.setStateNumbers(sepStates);
								pp_sep = new LinkedList<State>();
								dest_sep = new LinkedList<State>();
								pp_other_sep = new LinkedList<State>();
								bb_sep = new BitSet(sepStates.size());
								bb_other_sep = new BitSet(sepStates.size());
								pp_sep.clear();
								pp_sep.add(sepFSA.initial);
								if (shortestPrefix.size() > 0) {
									if (i > shortestPrefix.iterator().next()) {
										i = start = shortestPrefix.iterator().next();
										shortestPrefix.remove(shortestPrefix.iterator().next());

									}
									else {
										i = i + 1;
										start = i;
									}
								} else {
									//System.out.println("CHECKING TO BUILD CHART WITH CHARACTER => "+c);

									/**
									 * Checking to build Chart
									 */

									//System.out.println("RIGHT");
									String key = new StringBuilder().append(c).toString();
									if (i<str.length()-1){
										if (!sepFSA.run(str.substring(i+1,i+2)) || str.substring(i+1,i+2).equals(" ")) {
											if (specifications.get(key).getEOS()) {
												int startIndex = arcs.get(arcs.size() - 1).getStartIndex();
												int endIndex = arcs.get(arcs.size() - 1).getEndIndex();
												boolean buildChart = true;
												for (int indicesArcs = 0; indicesArcs < arcs.size(); indicesArcs++) {
													if (arcs.get(indicesArcs).getStartIndex() <= startIndex && endIndex < arcs.get(indicesArcs).getEndIndex() && arcs.get(indicesArcs).getIsKnown()==true) {
														buildChart = false;
													}
												}
												//System.out.println("CHARACTER IS==>"+i);
												if (i <str.length() - 2) {
													c_next = str.charAt(i + 1);
													char c_next_next = str.charAt(i+2);
													//System.out.println("c_next= "+c_next+" c_next_next= "+c_next_next);
													if ((c_next != ' ' && c_next!='\"' && c_next!='\'' && c_next!=')' && c_next!='(' && c_next!=']' && c_next!='[' && c_next!='}' && c_next!='{'  )|| c_next_next == '-'  ||  Character.isLowerCase(c_next_next) || c_next_next == '\'' || c_next_next == ',' || c_next_next == '”' || Character.isDigit(c_next_next)) {
														//System.out.println("FALSE");
														buildChart = false;
													}
												}
												if (i == str.length() - 2){
													buildChart = false;
												}
												if (buildChart == true) {
													//System.out.println("BUILDING CHART");
													counter++;
													ArrayList<ArrayList<BasicOperations.Edge>> chart = new ArrayList<ArrayList<BasicOperations.Edge>>();
													ArrayList<BasicOperations.Edge> bottomChartCopy = bottomChart;
													chart.add(bottomChartCopy);


													for (int k=1;k<bottomChart.size();k++){
														ArrayList<BasicOperations.Edge> partChart = new ArrayList<BasicOperations.Edge>();
														for (int l=0;l<bottomChart.size()-k;l++) {
															startIndex = bottomChart.get(l).getStartIndex();
															endIndex = bottomChart.get(l+k).getEndIndex();
															BasicOperations.Edge edge = new BasicOperations.Edge(startIndex,endIndex,true);
															if (arcs.contains(edge)){
																partChart.add(edge);
															}
															else {
																partChart.add(null);
															}

														}
														chart.add(partChart);
													}
													charts.add(chart);
													bottomChart = new ArrayList<BasicOperations.Edge>();
												}
											}
										}
									}
									start = i = i + 1;


								}

							} else {
								bottomChartFlag = true;
								shortestPrefix.add(i+1);
								i = i + 1;
							}
						}
					} else{
						if (i == str.length() - 1) {
							ArrayList<ArrayList<BasicOperations.Edge>> chart = new ArrayList<ArrayList<BasicOperations.Edge>>();
							ArrayList<BasicOperations.Edge> bottomChartCopy = bottomChart;
							chart.add(bottomChartCopy);

							for (int k = 1; k < bottomChart.size(); k++) {
								ArrayList<BasicOperations.Edge> partChart = new ArrayList<BasicOperations.Edge>();
								for (int l = 0; l < bottomChart.size() - k; l++) {
									int startIndex = bottomChart.get(l).getStartIndex();
									int endIndex = bottomChart.get(l + k).getEndIndex();
									BasicOperations.Edge edge = new BasicOperations.Edge(startIndex, endIndex, true);
									if (arcs.contains(edge)) {
										partChart.add(edge);
									} else {
										partChart.add(null);
									}

								}
								chart.add(partChart);
							}
							charts.add(chart);
						}
						i = i + 1;
					}
				} else{
					//System.out.println("CHECKING TO BUILD CHART WITH CHARACTER => "+c);
					//System.out.println("CHARACTER= "+c);
					if (i == str.length() - 1) {
						ArrayList<ArrayList<BasicOperations.Edge>> chart = new ArrayList<ArrayList<BasicOperations.Edge>>();
						ArrayList<BasicOperations.Edge> bottomChartCopy = bottomChart;
						chart.add(bottomChartCopy);

						for (int k = 1; k < bottomChart.size(); k++) {
							ArrayList<BasicOperations.Edge> partChart = new ArrayList<BasicOperations.Edge>();
							for (int l = 0; l < bottomChart.size() - k; l++) {
								int startIndex = bottomChart.get(l).getStartIndex();
								int endIndex = bottomChart.get(l + k).getEndIndex();
								BasicOperations.Edge edge = new BasicOperations.Edge(startIndex, endIndex, true);
								if (arcs.contains(edge)) {
									partChart.add(edge);
								} else {
									partChart.add(null);
								}

							}
							chart.add(partChart);
						}
						charts.add(chart);
						//System.out.println("ADDED CHART>>> "+charts.size());
					}
					/**
					 * CASE 3: Unknown Input
					 */

					//System.out.println("UNKNOWN INPUT>>>"+c);
					arcs.add(new Edge(start,i+1,false));
					start = i = i + 1;

				}
			}

		}

		return charts;

	}

	public static ArrayList<BasicOperations.Edge> traverseImplicitSolution(Automaton nonSep, Automaton sep, String str) {
		ArrayList<ArrayList<BasicOperations.Edge>> chart = new ArrayList<ArrayList<BasicOperations.Edge>>();

		// 1. Trying with each FSA to determine which one is the right one to start with and store it in workingAutomaton

		int sizeDestNonSep = 0,sizeDestSep=0; // To check whether there is a transition here for that character using each FSA
		int i = 0;
		char c;
		ArrayList<BasicOperations.Edge> indices = new ArrayList<BasicOperations.Edge>();

		Automaton workingAutomaton = null, nextAutomaton= null;

		while(sizeDestNonSep==0 && sizeDestSep==0 && i<str.length()) {
			sizeDestNonSep = sizeDestSep = 0; // Setting Counters to 0
			c = str.charAt(i);
			//System.out.println("Character " + c);

			// 1.1. Trying with nonSep FSA
			Set<State> states = nonSep.getStates();
			Automaton.setStateNumbers(states);
			LinkedList<State> pp = new LinkedList<State>();
			LinkedList<State> dest = new LinkedList<State>();
			pp.add(nonSep.initial);

			for (State p : pp) {
				dest.clear();
				p.step(c, dest);
				sizeDestNonSep += dest.size();
			}

			// 1.2. Trying with sep FSA
			states = sep.getStates();
			Automaton.setStateNumbers(states);
			pp = new LinkedList<State>();
			dest = new LinkedList<State>();

			pp.add(sep.initial);

			for (State p : pp) {
				dest.clear();
				p.step(c, dest);
				sizeDestSep += dest.size();
			}


			if (sizeDestNonSep != 0) {
				//System.out.println("sizeDestNonSep != 0");
				workingAutomaton = nonSep;
				nextAutomaton = sep;
			} else if (sizeDestSep != 0) {
				//System.out.println("sizeDestSep != 0");
				workingAutomaton = sep;
				nextAutomaton = nonSep;
			} else if (sizeDestNonSep == 0 && sizeDestSep==0) {
				//System.out.println("sizeDestNonSep == 0 && sizeDestSep==0");
				indices.add(new Edge(i, i + 1, false));
				i = i + 1;
			}
		}


		Set<Integer> shortestPrefix = new HashSet<Integer>();

		// For Building the dynamic Chart
		ArrayList<BasicOperations.Edge> bottomChart = new ArrayList<BasicOperations.Edge>();
		int start = i;
		boolean accept = false;
		boolean stuck = false;


		if (workingAutomaton != null) {
			Set<State> states = workingAutomaton.getStates();
			Automaton.setStateNumbers(states);
			LinkedList<State> pp = new LinkedList<State>();
			LinkedList<State> dest = new LinkedList<State>();
			LinkedList<State> pp_other = new LinkedList<State>();
			BitSet bb = new BitSet(states.size());
			BitSet bb_other = new BitSet(states.size());

			pp.add(workingAutomaton.initial);

			//printLinkedList(pp);


			while (i < str.length()) { // Check for the end of tokenization condition
				// 2. Traverse the FSA to find the next states from that character using the working Automaton
				accept = false;
				c = str.charAt(i);

				pp_other.clear();
				bb_other.clear();
				System.out.println("Character " + c);
				/*
				System.out.println("START OF PRINTING AUTOMATON");
				if (c==' ') {
					System.out.println(workingAutomaton.toString());
				}
				System.out.println("END OF PRINTING AUTOMATON");
				printLinkedList(pp);
				*/
				for (State p : pp) {
					dest.clear();
					p.step(c, dest);
					for (State q : dest) {
						if (q.accept) {
							accept = true;
						}
						if (!bb_other.get(q.number)) {
							bb_other.set(q.number);
							pp_other.add(q);
						}
					}
				}

				LinkedList<State> tp = pp;
				pp = pp_other;
				pp_other = tp;

				BitSet tb = bb;
				bb = bb_other;
				bb_other = tb;

				//printLinkedList(pp);
				//System.out.println("---------------------------------------------------------");

				if (accept == true) {
					System.out.println("Final State");
					LinkedList<State> ppSep = new LinkedList<State>();
					int sizeDest = 0;
					ppSep.add(nextAutomaton.initial);
					//printLinkedList(ppSep);
					if (i < str.length() - 1) {
						c = str.charAt(i + 1);
						for (State p : ppSep) {
							dest.clear();
							p.step(c, dest);
							//printLinkedList(dest);
							sizeDest += dest.size();
						}

						if (sizeDest == 0) {
							System.out.println("Didn't find separator");
							i = i + 1;
						} else {
							System.out.println("Found SEPARATOR");
							indices.add(new Edge(start, i, true));
							// Check if it is stuck
							sizeDest = 0;
							c = str.charAt(i + 1);
							System.out.println(c);

							//System.out.println("kkkkkkkkkkkkkkk");
							//pp.clear();
							for (State p : pp) {
								//System.out.println(p);
								dest.clear();
								p.step(c, dest);
								//printLinkedList(dest);
								sizeDest += dest.size();
							}
							if (sizeDest == 0) { // Stuck
								System.out.println("STUCK STOP HERE");
								// Swap nextAutomaton and workingAutomaton
								Automaton tpAutomaton = workingAutomaton;
								workingAutomaton = nextAutomaton;
								nextAutomaton = tpAutomaton;

								pp.clear();
								pp.add(workingAutomaton.initial);
								/*
								System.out.println("<<<<<<<PRINTING PPPPPPPPPP");
								printLinkedList(pp);
								System.out.println("PRINTING PPPPPPPPPP>>>>>");
								*/
								states = workingAutomaton.getStates();
								Automaton.setStateNumbers(states);
								bb = new BitSet(states.size());
								bb_other = new BitSet(states.size());

								System.out.println("shortestPrefix.size()="+shortestPrefix.size());
								if (shortestPrefix.size() > 0) {
									if (i > shortestPrefix.iterator().next()) {
										i = start = shortestPrefix.iterator().next();
										shortestPrefix.remove(shortestPrefix.iterator().next());
									}
									else {
										i = i + 1;
										start = i;
									}
								}
								else {
									i = i + 1;
									start = i;
								}

							} else { // Not Stuck
								System.out.println("NOT STUCK CONTINUING FOR LONGEST MATCH");
								shortestPrefix.add(i+1);
								i = i + 1;

							}

						}
					} else {
						System.out.println("start= "+start+" i= "+i);
						indices.add(new Edge(start, i, true));
						System.out.println("shortestPrefix.size()="+shortestPrefix.size());
						if (shortestPrefix.size()>0) {
							// Swap nextAutomaton and workingAutomaton
							Automaton tpAutomaton = workingAutomaton;
							workingAutomaton = nextAutomaton;
							nextAutomaton = tpAutomaton;

							pp.clear();
							pp.add(workingAutomaton.initial);
							/*
							System.out.println("<<<<<<<PRINTING PPPPPPPPPP");
							printLinkedList(pp);
							System.out.println("PRINTING PPPPPPPPPP>>>>>");*/
							states = workingAutomaton.getStates();
							Automaton.setStateNumbers(states);
							bb = new BitSet(states.size());
							bb_other = new BitSet(states.size());
							i = start = shortestPrefix.iterator().next();
							System.out.println(i);
							shortestPrefix.remove(shortestPrefix.iterator().next());
						}
					}

				} else {
					i = i + 1;
				}
			}
		}

		/*chart.add(bottomChart);

		for (int k = 1; k < bottomChart.size(); k++) {
			ArrayList<BasicOperations.Edge> partChart = new ArrayList<BasicOperations.Edge>();
			for (int l = 0; l < bottomChart.size() - k; l++) {
				int startIndex = bottomChart.get(l).getStartIndex();
				int endIndex = bottomChart.get(l + k).getEndIndex();
				BasicOperations.Edge edge = new BasicOperations.Edge(startIndex, endIndex, true);
				if (indices.contains(edge)) {
					partChart.add(edge);
				} else {
					partChart.add(null);
				}

			}
			chart.add(partChart);
		}*/

		return indices;
	}


	public static Graph buildGraph(Automaton a, ArrayList<ArrayList<String>> tokens){
		Node node = new Node(0,"ROOT");
		Graph tokensGraph = new Graph();
		List<Node> parentsList = new ArrayList<Node>();
		int counter = 0;
		parentsList.add(node);
		tokensGraph.addNode(node);
		for (int i=0;i<tokens.size();i++){
			// Check each element in the list of tokens if it has a list of indices
			if(tokens.get(i).size()==1){ // Case 1: it doesn't have it
				// Build a tree by adding a node to the tree
				counter ++;
				Node newNode = new Node(counter,tokens.get(i).get(0));
				for (int parent=0;parent<parentsList.size();parent++) {
					tokensGraph.addEdge(parentsList.get(parent),newNode);
				}
				parentsList.clear();
				parentsList.add(newNode);
			}
			else { // Case 2: it has a list of indices
				// Search for all possible subset tokenizations
				ArrayList<String> tokenList = new ArrayList<String>();
				String tokenStr = tokens.get(i).get(0);
				List<String> indices = tokens.get(i).subList(1,tokens.get(i).size());
				tokenList.add(tokenStr.substring(0,Integer.parseInt(indices.get(0))));
				int nextIndex = Integer.parseInt(indices.get(0));
				for (int j=0;j<indices.size();j++){
					if (j==indices.size()-1){
						tokenList.add(tokenStr.substring(nextIndex,tokenStr.length()));
					}
					else {
						tokenList.add(tokenStr.substring(nextIndex,Integer.parseInt(indices.get(j+1))));
						nextIndex = Integer.parseInt(indices.get(j+1));
					}
				}
				NbitsStrings bitsStr = new NbitsStrings(indices.size());
				ArrayList<ArrayList<Integer>> nbitsComb = bitsStr.nBits(indices.size());

				ArrayList<List<String>> partitionsList = new ArrayList<List<String>>();
				//System.out.println(indices);
				//System.out.println(tokenList);

				ArrayList<Integer> newIndices = new ArrayList<Integer>();
				for (int j=0;j<indices.size();j++){
					newIndices.add(Integer.parseInt(indices.get(j))+j);
				}

				String tokenJoined = String.join(",",tokenList);
				//System.out.println("Printed tokenJoined>>>>>"+tokenJoined);
				//System.out.println("Printed newIndices>>>>>"+newIndices);
				for (int j=0;j<nbitsComb.size();j++){
					for (int l=0;l<indices.size();l++){
						newIndices.set(l,Integer.parseInt(indices.get(l))+l);
					}
					tokenJoined = String.join(",",tokenList);
					//System.out.println("Case nbitsComb >>>>"+nbitsComb.get(j));
					for (int k=0;k<nbitsComb.get(j).size();k++){
						if (nbitsComb.get(j).get(k)==0){
							tokenJoined = tokenJoined.substring(0,newIndices.get(k))+tokenJoined.substring(newIndices.get(k)+1);
							for (int l=k;l<newIndices.size();l++){
								newIndices.set(l,newIndices.get(l)-1);
							}
						}
						//System.out.println("tokenJoined:"+tokenJoined);
					}
					// Transform partition to a list by adding the splittings of the string
					List<String> partition = Arrays.asList(tokenJoined.split(","));
					//System.out.println("partition >>>>"+partition);
					partitionsList.add(partition);
				}
				//System.out.println("partitionsList");

				//System.out.println(partitionsList);
				ArrayList<Integer> indicesRemove = new ArrayList<Integer>();
				for (int j=0;j<partitionsList.size();j++){
					for (int k=0;k<partitionsList.get(j).size();k++){
							String partition = partitionsList.get(j).get(k);
							//System.out.println(partition);
							if (run(a, partition)==false){
								//System.out.println("FALSE RUN");
								indicesRemove.add(j);
								break;
							}
					}

				}
				Collections.sort(indicesRemove);
				int [] arrIndicesRemove = new int [indicesRemove.size()];
				int p = 0;
				for (int index=0;index<indicesRemove.size();index++){
					arrIndicesRemove[p] = indicesRemove.get(index);
					p++;
				}
				System.out.println(partitionsList);
				for (int j = arrIndicesRemove.length-1; j >= 0; j--) {
					partitionsList.remove(arrIndicesRemove[j]);
				}

				//System.out.println(partitionsList);
				List<Node> newParentsList = new ArrayList<Node>();
				for (int parent=0;parent<parentsList.size();parent++) {
					Node parentCopy;
					for (int j = 0; j < partitionsList.size(); j++) {
						parentCopy = parentsList.get(parent);
						for (int k = 0; k < partitionsList.get(j).size(); k++) {
							counter ++;
							Node newNode = new Node(counter,partitionsList.get(j).get(k));
							tokensGraph.addNode(newNode);
							tokensGraph.addEdge(parentCopy,newNode);
							parentCopy = newNode;
						}
						newParentsList.add(parentCopy);
					}
				}

				parentsList.clear();
				parentsList.addAll(newParentsList);
			}
		}

		return tokensGraph;


	}

	/*
		private Node<T> root;
		for (int m=0;m<tokens.size();m++){
			System.out.println(positionList);
			if (!positionList.contains(String.valueOf(m))){
				System.out.println("Token: "+tokens.get(m)+" doesn't have replacement");
				// Add one leaf node to the leaves of the tree
			}
			else {
				System.out.println("Token: "+tokens.get(m)+"has replacement");
			}
		}*/
	//Map<ArrayList<String>,ArrayList<String>> tokensCandidates = HashMap <Map<ArrayList<String>,ArrayList<String>>();
	//tokensCandidates.put(tokens,candidates);
	public static void printBitSet(BitSet bi){
		StringBuilder s = new StringBuilder();
		for( int i = 0; i < bi.length();  i++ )
		{
			s.append( bi.get( i ) == true ? 1: 0 );
		}

		System.out.println( s );

	}
	public static List<String> traverseShortest(Automaton a, String s) {
		ArrayList<String> tokens = new ArrayList<String>();

		return tokens;
	}


	public static String getStringRepresentation(ArrayList<Character> list) {
		StringBuilder builder = new StringBuilder(list.size());
		for(Character ch: list)
		{
			builder.append(ch);
		}
		return builder.toString();
	}
	public static void printLinkedList(LinkedList<State> linkedList){
		for (int i=0;i<linkedList.size();i++){
			System.out.println(linkedList.get(i));
		}
	}

	public static ArrayList<ArrayList<String>> buildChart(Automaton a, ArrayList<ArrayList<String>> tokens){
		// 1. Construct the list of words at the basis of the chart
		ArrayList<String> words = new ArrayList<String>();
		for (int i=0;i<tokens.size();i++) {
			// Check each element in the list of tokens if it has a list of indices
			if (tokens.get(i).size() > 2) { // Case 1: it doesn't have it
				// Build a tree by adding a node to the tree
				if (i == 0) { // take both start and end separators
					int index1 = Integer.parseInt(tokens.get(i).get(1));
					String sep1 = tokens.get(i).get(0).substring(0, index1 + 1);
					words.add(sep1);
					int index2 = Integer.parseInt(tokens.get(i).get(tokens.get(i).size()-1));
					int k = 0;
					for (String index: tokens.get(i).subList(2,tokens.get(i).size())) {
						String nonsep = tokens.get(i).get(0).substring(k+1,Integer.parseInt(index)); // .substring(index1+1,index2);
						words.add(nonsep);
						k = Integer.parseInt(index);
						String sep = tokens.get(i).get(0).substring(k, k+1);
						words.add(sep);
					}
				} else { // take only end separator
					int index2 = Integer.parseInt(tokens.get(i).get(tokens.get(i).size()-1));
					int k = 0;
					for (String index: tokens.get(i).subList(2,tokens.get(i).size())) {
						System.out.println("index="+index);
						String nonsep = tokens.get(i).get(0).substring(k+1,Integer.parseInt(index)); // .substring(index1+1,index2);
						words.add(nonsep);
						k = Integer.parseInt(index);
						String sep = tokens.get(i).get(0).substring(k, k+1);
						words.add(sep);
					}
				}
			}
			else {
				String nonsep = tokens.get(i).get(0);
				words.add(nonsep);
			}
		}

		int length = words.size();
		ArrayList<String>[][] table = new ArrayList[length][];

		for (int i = 0; i < length; ++i) {
			table[i] = new ArrayList[length];
			for (int j = 0; j < length; ++j) {
				table[i][j] = new ArrayList<String>();
			}
		}
		int i = 0;
		for (String word : words) {
			table[0][i].add(word);
			i ++;
		}

		// Embedding elements of Upper levels in the chart
		int counter = 0;
		int nextLayer=0;
		ArrayList<Integer> indices = new ArrayList<Integer>();
		for (i=0;i<tokens.size();i++) {
			if (tokens.get(i).size() > 2) {
				nextLayer ++;
				counter += Integer.parseInt(tokens.get(i).get(tokens.get(i).size()-1));
				indices.add(counter);
			}
		}
		if(nextLayer>0) {
			ArrayList<Integer> chartIndices = new ArrayList<Integer>();
			System.out.println("indices:" + indices);
			String accumulator = "";
			int index = indices.get(0);
			int increment = 1;
			for (int k = 0; k < words.size(); k++) {
				accumulator = new StringBuilder().append(accumulator).append(words.get(k)).toString();
				System.out.println("accumulator: " + accumulator);
				System.out.println("increment: " + increment);
				int accLength = accumulator.length() - 1;
				System.out.println("accumulator.length()-1: " + accLength);
				if (accumulator.length() - 1 == index) {
					chartIndices.add(k);
					if (increment < indices.size()) {
						index = indices.get(increment);
						increment++;
					}
				}
			}

			System.out.println("chartIndices:" + chartIndices);
			int start = 0;
			for (Integer chartIndex : chartIndices) {
				System.out.println("Chart Index>>>>" + chartIndex);
				for (int l = 1; l < chartIndex + 1; l++) {
					for (int p = start; p < chartIndex + 1 - l; p++) {
						int sum = p + l;
						System.out.println("p=" + p + " p+l=" + sum);
						String candidate = table[0][p].get(0);
						for (int m = p + 1; m < p + l + 1; m++) {
							candidate = new StringBuilder().append(candidate).append(table[0][m].get(0)).toString();
						}
						System.out.println("Candidate: " + candidate);
						if (run(a, candidate) == true) {
							System.out.println("Adding at p="+p+" and l="+l+" to chart>>>>"+candidate);
							table[p + l][p].add(candidate.substring(1, candidate.length() - 1));
						}
					}
				}
				start = chartIndex;
			}
		}

		System.out.println("Length of table:"+table.length);

		ArrayList<ArrayList<String>> chart = new ArrayList<ArrayList<String>>();
		for (int h=0;h<length;h++) {
			ArrayList<String> subChart = new ArrayList<String> ();
			for (int j = 0; j < length; ++j) {
				if (table[h][j].size()>0) {
					subChart.add(table[h][j].get(0));
				}else{
					subChart.add("");
				}
			}
			chart.add(subChart);
		}

		return chart;
	}
}