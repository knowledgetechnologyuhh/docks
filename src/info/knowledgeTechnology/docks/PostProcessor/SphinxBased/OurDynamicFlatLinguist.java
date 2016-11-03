/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 * Modified by Johannes Twiefel
 */
package info.knowledgeTechnology.docks.PostProcessor.SphinxBased;

import info.knowledgeTechnology.docks.Data.PhoneData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import edu.cmu.sphinx.decoder.scorer.ScoreProvider;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.linguist.Linguist;
import edu.cmu.sphinx.linguist.SearchGraph;
import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.SearchStateArc;
import edu.cmu.sphinx.linguist.UnitSearchState;
import edu.cmu.sphinx.linguist.WordSearchState;
import edu.cmu.sphinx.linguist.WordSequence;
import edu.cmu.sphinx.linguist.acoustic.AcousticModel;
import edu.cmu.sphinx.linguist.acoustic.HMMPosition;
import edu.cmu.sphinx.linguist.acoustic.Unit;
import edu.cmu.sphinx.linguist.acoustic.UnitManager;
import edu.cmu.sphinx.linguist.dictionary.Pronunciation;
import edu.cmu.sphinx.linguist.dictionary.Word;
import edu.cmu.sphinx.linguist.language.grammar.Grammar;
import edu.cmu.sphinx.linguist.language.grammar.GrammarArc;
import edu.cmu.sphinx.linguist.language.grammar.GrammarNode;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.Timer;
import edu.cmu.sphinx.util.TimerPool;
import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Boolean;
import edu.cmu.sphinx.util.props.S4Component;
import edu.cmu.sphinx.util.props.S4Double;
/**
 * A simple form of the linguist. It makes the following simplifying assumptions: 1) Zero or one word per grammar node
 * 2) No fan-in allowed ever 3) No composites (yet) 4) Only Unit, HMMState, and pronunciation states (and the
 * initial/final grammar state are in the graph (no word, alternative or grammar states attached). 5) Only valid
 * transitions (matching contexts) are allowed 6) No tree organization of units 7) Branching grammar states are
 * allowed
 * <p/>
 * This is a dynamic version of the flat linguist that is more efficient in terms of startup time and overall footprint
 * <p/>
 * Note that all probabilities are maintained in the log math domain
 */

public class OurDynamicFlatLinguist implements Linguist, Configurable {

    /** The property used to define the grammar to use when building the search graph */
    @S4Component(type = Grammar.class)
    public final static String GRAMMAR = "grammar";

    /** The property used to define the unit manager to use when building the search graph */
    @S4Component(type = UnitManager.class)
    public final static String UNIT_MANAGER = "unitManager";

    /** The property used to define the acoustic model to use when building the search graph */
    @S4Component(type = AcousticModel.class)
    public final static String ACOUSTIC_MODEL = "acousticModel";

    /** The property that specifies whether to add a branch for detecting out-of-grammar utterances. */
    @S4Boolean(defaultValue = false)
    public final static String ADD_OUT_OF_GRAMMAR_BRANCH = "addOutOfGrammarBranch";

    /** The property for the probability of entering the out-of-grammar branch. */
    @S4Double(defaultValue = 1.0)
    public final static String OUT_OF_GRAMMAR_PROBABILITY = "outOfGrammarProbability";

    /** The property for the probability of inserting a CI phone in the out-of-grammar ci phone loop */
    @S4Double(defaultValue = 1.0)
    public static final String PHONE_INSERTION_PROBABILITY = "phoneInsertionProbability";

    /** The property for the acoustic model to use to build the phone loop that detects out of grammar utterances. */
    @S4Component(type = AcousticModel.class)
    public final static String PHONE_LOOP_ACOUSTIC_MODEL = "phoneLoopAcousticModel";



    // ----------------------------------
    // Subcomponents that are configured
    // by the property sheet
    // -----------------------------------
    private Grammar grammar;
    private AcousticModel acousticModel;
    private AcousticModel phoneLoopAcousticModel;
    private LogMath logMath;
    // ------------------------------------
    // Data that is configured by the
    // property sheet
    // ------------------------------------
    private float logWordInsertionProbability;
    private float logSilenceInsertionProbability;
    private float logUnitInsertionProbability;
    private float logFillerInsertionProbability;
    private float languageWeight;
    @SuppressWarnings("unused")
    private float logOutOfGrammarBranchProbability;
    private float logPhoneInsertionProbability;
    private boolean addOutOfGrammarBranch;

    // ------------------------------------
    // Data used for building and maintaining
    // the search graph
    // -------------------------------------
    private SearchGraph searchGraph;
    private Logger logger;
    SearchStateArc outOfGrammarGraph;
    private GrammarNode initialGrammarState;

    // this map is used to manage the set of follow on units for a
    // particular grammar node. It is used to select the set of
    // possible right contexts as we leave a node

    private Map<GrammarNode, int[]> nodeToNextUnitArrayMap;

    // this map is used to manage the set of possible entry units for
    // a grammar node. It is used to filter paths so that we only
    // branch to grammar nodes that match the current right context.

    private Map<GrammarNode, Set<Unit>> nodeToUnitSetMap;

    // an empty arc (just waiting for Noah, I guess)
    private final SearchStateArc[] EMPTY_ARCS = new SearchStateArc[0];

    public OurDynamicFlatLinguist(AcousticModel acousticModel, Grammar grammar, 
            double wordInsertionProbability, double silenceInsertionProbability, double unitInsertionProbability,
            double fillerInsertionProbability, float languageWeight, boolean addOutOfGrammarBranch,
            double outOfGrammarBranchProbability, double phoneInsertionProbability, AcousticModel phoneLoopAcousticModel) {

        this.logger = Logger.getLogger(getClass().getName());
        this.acousticModel = acousticModel;
        logMath = LogMath.getInstance();
        this.grammar = grammar;

        this.logWordInsertionProbability = logMath.linearToLog(wordInsertionProbability);
        this.logSilenceInsertionProbability = logMath.linearToLog(silenceInsertionProbability);
        this.logUnitInsertionProbability = logMath.linearToLog(unitInsertionProbability);
        this.logFillerInsertionProbability = logMath.linearToLog(fillerInsertionProbability);
        this.languageWeight = languageWeight;
        this.addOutOfGrammarBranch = addOutOfGrammarBranch;
        this.logOutOfGrammarBranchProbability = logMath.linearToLog(outOfGrammarBranchProbability);

        this.logPhoneInsertionProbability = logMath.linearToLog(logPhoneInsertionProbability);
        if (addOutOfGrammarBranch) {
            this.phoneLoopAcousticModel = phoneLoopAcousticModel;
        }
    }

    public OurDynamicFlatLinguist() {
    }

    /*
    * (non-{
public class DynamicFlatLinguist Javadoc)
    *
    * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
    */
    @Override
    public void newProperties(PropertySheet ps) throws PropertyException {
        // hookup to all of the components
        logger = ps.getLogger();
        acousticModel = (AcousticModel) ps.getComponent(ACOUSTIC_MODEL);

        logMath = LogMath.getInstance();
        grammar = (Grammar) ps.getComponent(GRAMMAR);

        // get the rest of the configuration data
        logWordInsertionProbability = logMath.linearToLog(ps.getDouble(PROP_WORD_INSERTION_PROBABILITY));
        logSilenceInsertionProbability = logMath.linearToLog(ps.getDouble(PROP_SILENCE_INSERTION_PROBABILITY));
        logUnitInsertionProbability = logMath.linearToLog(ps.getDouble(PROP_UNIT_INSERTION_PROBABILITY));
        logFillerInsertionProbability = logMath.linearToLog(ps.getDouble(PROP_FILLER_INSERTION_PROBABILITY));
        languageWeight = ps.getFloat(Linguist.PROP_LANGUAGE_WEIGHT);
        addOutOfGrammarBranch = ps.getBoolean(ADD_OUT_OF_GRAMMAR_BRANCH);
        logOutOfGrammarBranchProbability = logMath.linearToLog(ps.getDouble(OUT_OF_GRAMMAR_PROBABILITY));
        
        logPhoneInsertionProbability = logMath.linearToLog(ps.getDouble(PHONE_INSERTION_PROBABILITY));
        if (addOutOfGrammarBranch) {
            phoneLoopAcousticModel = (AcousticModel) ps.getComponent(PHONE_LOOP_ACOUSTIC_MODEL);
        }
    }


    /**
     * Returns the search graph
     *
     * @return the search graph
     */
    @Override
    public SearchGraph getSearchGraph() {
        return searchGraph;
    }


    /**
     * Sets up the acoustic model.
     *
     * @param ps the PropertySheet from which to obtain the acoustic model
     * @throws edu.cmu.sphinx.util.props.PropertyException
     */
    protected void setupAcousticModel(PropertySheet ps)
            throws PropertyException {
        acousticModel = (AcousticModel) ps.getComponent(ACOUSTIC_MODEL);
    }


    @Override
    public void allocate() throws IOException {
        logger.info("Allocating DFLAT");
        allocateAcousticModel();
        grammar.allocate();
        nodeToNextUnitArrayMap = new HashMap<GrammarNode, int[]>();
        nodeToUnitSetMap = new HashMap<GrammarNode, Set<Unit>>();
        Timer timer = TimerPool.getTimer(this, "compileGrammar");
        timer.start();
        compileGrammar();
        timer.stop();
        logger.info("Done allocating  DFLAT");
    }


    /** Allocates the acoustic model.
     * @throws java.io.IOException*/
    protected void allocateAcousticModel() throws IOException {
        acousticModel.allocate();
        if (addOutOfGrammarBranch) {
            phoneLoopAcousticModel.allocate();
        }
    }


    /*
    * (non-Javadoc)
    *
    * @see edu.cmu.sphinx.linguist.Linguist#deallocate()
    */
    @Override
    public void deallocate() {
        if (acousticModel != null) {
            acousticModel.deallocate();
        }
        grammar.deallocate();
    }


    /** Called before a recognition */
    @Override
    public void startRecognition() {
        if (grammarHasChanged()) {
            compileGrammar();
        }
    }


    /** Called after a recognition */
    @Override
    public void stopRecognition() {
    }


    /**
     * Returns the log silence insertion probability.
     *
     * @return the log silence insertion probability.
     */
    public float getLogSilenceInsertionProbability() {
        return logSilenceInsertionProbability;
    }

    /**
     * Determines if the underlying grammar has changed since we last compiled the search graph
     *
     * @return true if the grammar has changed
     */
    private boolean grammarHasChanged() {
        return initialGrammarState == null ||
                initialGrammarState != grammar.getInitialNode();
    }

    private void compileGrammar() {
        initialGrammarState = grammar.getInitialNode();

        for (GrammarNode node : grammar.getGrammarNodes()) {
            initUnitMaps(node);
        }

        searchGraph = new DynamicFlatSearchGraph();
    }


    /**
     * Initializes the unit maps for this linguist. There are two unit maps: (a) nodeToNextUnitArrayMap contains an
     * array of unit ids for all possible units that immediately follow the given grammar node. This is used to
     * determine the set of exit contexts for words within a grammar node. (b) nodeToUnitSetMap contains the set of
     * possible entry units for a given grammar node. This is typically used to determine if a path with a given right
     * context should branch into a particular grammar node
     *
     * @param node the units maps will be created for this node.
     */
    private void initUnitMaps(GrammarNode node) {

        // collect the set of next units for this node

        if (nodeToNextUnitArrayMap.get(node) == null) {
            Set<GrammarNode> vistedNodes = new HashSet<GrammarNode>();
            Set<Unit> unitSet = new HashSet<Unit>();

            GrammarArc[] arcs = node.getSuccessors();
            for (GrammarArc arc : arcs) {
                GrammarNode nextNode = arc.getGrammarNode();
                collectNextUnits(nextNode, vistedNodes, unitSet);
            }
            int[] nextUnits = new int[unitSet.size()];
            int index = 0;
            for (Unit unit : unitSet) {
                nextUnits[index++] = unit.getBaseID();
            }
            nodeToNextUnitArrayMap.put(node, nextUnits);
        }

        // collect the set of entry units for this node

        if (nodeToUnitSetMap.get(node) == null) {
            Set<GrammarNode> vistedNodes = new HashSet<GrammarNode>();
            Set<Unit> unitSet = new HashSet<Unit>();
            collectNextUnits(node, vistedNodes, unitSet);
            nodeToUnitSetMap.put(node, unitSet);
        }
    }


    /**
     * For the given grammar node, collect the set of possible next units.
     *
     * @param thisNode    the grammar node
     * @param vistedNodes the set of visited grammar nodes, used to ensure that we don't attempt to expand a particular
     *                    grammar node more than once (which could lead to a death spiral)
     * @param unitSet     the entry units are collected here.
     */
    private void collectNextUnits(GrammarNode thisNode,
                                  Set<GrammarNode> vistedNodes, Set<Unit> unitSet) {
        if (vistedNodes.contains(thisNode)) {
            return;
        }

        vistedNodes.add(thisNode);
        if (thisNode.isFinalNode()) {
            unitSet.add(UnitManager.SILENCE);
        } else if (!thisNode.isEmpty()) {
            Word word = thisNode.getWord();
            Pronunciation[] pronunciations = word.getPronunciations();
            for (Pronunciation pronunciation : pronunciations) {
                unitSet.add(pronunciation.getUnits()[0]);
            }
        } else {
            GrammarArc[] arcs = thisNode.getSuccessors();
            for (GrammarArc arc : arcs) {
                GrammarNode nextNode = arc.getGrammarNode();
                collectNextUnits(nextNode, vistedNodes, unitSet);
            }
        }
    }


    final Map<SearchState, SearchStateArc[]> successorCache = new HashMap<SearchState, SearchStateArc[]>();

    /** The base search state for this dynamic flat linguist. */
    abstract class FlatSearchState implements SearchState, SearchStateArc {

        final static int ANY = 0;


        /**
         * Gets the set of successors for this state
         *
         * @return the set of successors
         */
        @Override
        public abstract SearchStateArc[] getSuccessors();


        /**
         * Returns a unique string representation of the state. This string is suitable (and typically used) for a label
         * for a GDL node
         *
         * @return the signature
         */
        @Override
        public abstract String getSignature();


        /**
         * Returns the order of this state type among all of the search states
         *
         * @return the order
         */
        @Override
        public abstract int getOrder();


        /**
         * Determines if this state is an emitting state
         *
         * @return true if this is an emitting state
         */
        @Override
        public boolean isEmitting() {
            return false;
        }


        /**
         * Determines if this is a final state
         *
         * @return true if this is a final state
         */
        @Override
        public boolean isFinal() {
            return false;
        }


        /**
         * Returns a lex state associated with the searc state (not applicable to this linguist)
         *
         * @return the lex state (null for this linguist)
         */
        @Override
        public Object getLexState() {
            return null;
        }


        /**
         * Returns a well formatted string representation of this state
         *
         * @return the formatted string
         */
        @Override
        public String toPrettyString() {
            return toString();
        }


        /**
         * Returns a string representation of this object
         *
         * @return a string representation
         */
        @Override
        public String toString() {
            return getSignature();
        }


        /**
         * Returns the word history for this state (not applicable to this linguist)
         *
         * @return the word history (null for this linguist)
         */
        @Override
        public WordSequence getWordHistory() {
            return null;
        }


        /**
         * Gets a successor to this search state
         *
         * @return the successor state
         */
        @Override
        public SearchState getState() {
            return this;
        }


        /**
         * Gets the composite probability of entering this state
         *
         * @return the log probability
         */
        @Override
        public float getProbability() {
            return getLanguageProbability() + getInsertionProbability();
        }


        /**
         * Gets the language probability of entering this state
         *
         * @return the log probability
         */
        @Override
        public float getLanguageProbability() {
            return LogMath.LOG_ONE;
        }
        

        /**
         * Gets the insertion probability of entering this state
         *
         * @return the log probability
         */
        @Override
        public float getInsertionProbability() {
            return LogMath.LOG_ONE;
        }


        /**
         * Get the arcs from the cache if the exist
         *
         * @return the cached arcs or null
         */
        SearchStateArc[] getCachedSuccessors() {
            return successorCache.get(this);
        }


        /**
         * Places the set of successor arcs in the cache
         *
         * @param successors the set of arcs to be cached for this state
         */
        void cacheSuccessors(SearchStateArc[] successors) {
            successorCache.put(this, successors);
        }
    }

    /**
     * Represents a grammar node in the search graph. A grammar state needs to keep track of the associated grammar node
     * as well as the left context and next base unit.
     */
    class GrammarState extends FlatSearchState {

        private final GrammarNode node;
        private final int lc;
        private final int nextBaseID;
        private final float languageProbability;


        /**
         * Creates a grammar state for the given node with a silence Lc
         *
         * @param node the grammar node
         */
        GrammarState(GrammarNode node) {
            this(node, LogMath.LOG_ONE, UnitManager.SILENCE.getBaseID());
        }


        /**
         * Creates a grammar state for the given node and left context. The path will connect to any possible next base
         *
         * @param node                the grammar node
         * @param languageProbability the probability of transistioning to this word
         * @param lc                  the left context for this path
         */
        GrammarState(GrammarNode node, float languageProbability, int lc) {
            this(node, languageProbability, lc, ANY);
        }


        /**
         * Creates a grammar state for the given node and left context and next base ID.
         *
         * @param node                the grammar node
         * @param languageProbability the probability of transistioning to this word
         * @param lc                  the left context for this path
         * @param nextBaseID          the next base ID
         */
        GrammarState(GrammarNode node, float languageProbability,
                     int lc, int nextBaseID) {
            this.lc = lc;
            this.nextBaseID = nextBaseID;
            this.node = node;
            this.languageProbability = languageProbability;
        }


        /**
         * Gets the language probability of entering this state
         *
         * @return the log probability
         */
        @Override
        public float getLanguageProbability() {
            return languageProbability * languageWeight;
        }


        /**
         * Generate a hashcode for an object. Equality for a  grammar state includes the grammar node, the lc and the
         * next base ID
         *
         * @return the hashcode
         */
        @Override
        public int hashCode() {
            return node.hashCode() * 17 + lc * 7 + nextBaseID;
        }


        /**
         * Determines if the given object is equal to this object
         *
         * @param o the object to test
         * @return <code>true</code> if the object is equal to this
         */
        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof GrammarState) {
                GrammarState other = (GrammarState) o;
                return other.node == node && lc == other.lc
                        && nextBaseID == other.nextBaseID;
            } else {
                return false;
            }
        }


        /**
         * Determines if this is a final state in the search graph
         *
         * @return true if this is a final state in the search graph
         */
        @Override
        public boolean isFinal() {
            return node.isFinalNode();
        }


        /**
         * Gets the set of successors for this state
         * 
         * @return the set of successors
         */
        @Override
        public SearchStateArc[] getSuccessors() {

            SearchStateArc[] arcs = getCachedSuccessors();

            if (arcs != null) {
                return arcs;
            }

            if (isFinal()) {
                arcs = EMPTY_ARCS;
            } else if (node.isEmpty()) {
                arcs = getNextGrammarStates(lc, nextBaseID);
            } else {
                Word word = node.getWord();
                Pronunciation[] pronunciations = word.getPronunciations();

                // This can potentially speedup computation
                // pronunciations = filter(pronunciations, nextBaseID);

                SearchStateArc[] nextArcs = new SearchStateArc[pronunciations.length];

                for (int i = 0; i < pronunciations.length; i++) {
                    nextArcs[i] = new PronunciationState(this,
                            pronunciations[i]);
                }
                arcs = nextArcs;
            }

            cacheSuccessors(arcs);
            return arcs;
        }


        /**
         * Gets the set of arcs to the next set of grammar states that match the given nextBaseID
         *
         * @param lc         the current left context
         * @param nextBaseID the desired next base ID

         */
        SearchStateArc[] getNextGrammarStates(int lc, int nextBaseID) {
            GrammarArc[] nextNodes = node.getSuccessors();
            nextNodes = filter(nextNodes, nextBaseID);
            SearchStateArc[] nextArcs = new SearchStateArc[nextNodes.length];

            for (int i = 0; i < nextNodes.length; i++) {
                GrammarArc arc = nextNodes[i];
                nextArcs[i] = new GrammarState(arc.getGrammarNode(),
                        arc.getProbability(), lc, nextBaseID);
            }
            return nextArcs;
        }


        /**
         * Returns a unique string representation of the state. This string is suitable (and typically used) for a label
         * for a GDL node
         *
         * @return the signature
         */
        @Override
        public String getSignature() {
            return "GS " + node + "-lc-";
        }


        /**
         * Returns the order of this state type among all of the search states
         *
         * @return the order
         */
        @Override
        public int getOrder() {
            return 1;
        }


        /**
         * Given a set of arcs and the ID of the desired next unit, return the set of arcs containing only those that
         * transition to the next unit
         *
         * @param arcs     the set of arcs to filter
         * @param nextBase the ID of the desired next unit

         */
        GrammarArc[] filter(GrammarArc[] arcs, int nextBase) {
        	assert nextBase == ANY;
            return arcs;
        }


        /**
         * Retain only the pronunciations that start with the unit indicated by
         * nextBase. This method can be used instead of filter to reduce search
         * space. It's not used by default but could potentially lead to a
         * decoding speedup.
         * 
         * @param p
         *            the set of pronunciations to filter
         * @param nextBase
         *            the ID of the desired initial unit
         */
        Pronunciation[] filter(Pronunciation[] pronunciations, int nextBase) {

            if (nextBase == ANY) {
                return pronunciations;
            }

            ArrayList<Pronunciation> filteredPronunciation = new ArrayList<Pronunciation>(
                    pronunciations.length);
            for (Pronunciation pronunciation : pronunciations) {
                if (pronunciation.getUnits()[0].getBaseID() == nextBase) {
                    filteredPronunciation.add(pronunciation);
                }
            }
            return filteredPronunciation
                    .toArray(new Pronunciation[filteredPronunciation.size()]);
        }

        /**
         * Gets the ID of the left context unit for this path
         *
         * @return the left context ID
         */
        int getLC() {
            return lc;
        }


        /**
         * Gets the ID of the desired next unit
         *
         * @return the ID of the next unit
         */
        int getNextBaseID() {
            return nextBaseID;
        }


        /**
         * Returns the set of IDs for all possible next units for this grammar node
         *
         * @return the set of IDs of all possible next units
         */
        int[] getNextUnits() {
            return nodeToNextUnitArrayMap.get(node);
        }


        /**
         * Returns a string representation of this object
         *
         * @return a string representation
         */
        @Override
        public String toString() {
            return node + "[" ; //+ hmmPool.getUnit(lc) + ',' + hmmPool.getUnit(nextBaseID) + ']';
        }


        /**
         * Returns the grammar node associated with this grammar state
         *
         * @return the grammar node
         */
        GrammarNode getGrammarNode() {
            return node;
        }

    }

    class InitialState extends FlatSearchState {

        private final List<SearchStateArc> nextArcs = new ArrayList<SearchStateArc>();


        /**
         * Gets the set of successors for this state
         *
         * @return the set of successors
         */
        @Override
        public SearchStateArc[] getSuccessors() {
            return nextArcs.toArray(new
                    SearchStateArc[nextArcs.size()]);
        }


        public void addArc(SearchStateArc arc) {
            nextArcs.add(arc);
        }


        /**
         * Returns a unique string representation of the state. This string is suitable (and typically used) for a label
         * for a GDL node
         *
         * @return the signature
         */
        @Override
        public String getSignature() {
            return "initialState";
        }


        /**
         * Returns the order of this state type among all of the search states
         *
         * @return the order
         */
        @Override
        public int getOrder() {
            return 1;
        }


        /**
         * Returns a string representation of this object
         *
         * @return a string representation
         */
        @Override
        public String toString() {
            return getSignature();
        }
    }

    /** This class representations a word punctuation in the search graph */
    class PronunciationState extends FlatSearchState implements
            WordSearchState {

        private final GrammarState gs;
        private final Pronunciation pronunciation;


        /**
         * Creates a PronunciationState
         *
         * @param gs the associated grammar state
         * @param p  the pronunciation
         */
        PronunciationState(GrammarState gs, Pronunciation p) {
            this.gs = gs;
            this.pronunciation = p;
        }


        /**
         * Gets the insertion probability of entering this state
         *
         * @return the log probability
         */
        @Override
        public float getInsertionProbability() {
            if (pronunciation.getWord().isFiller()) {
                return LogMath.LOG_ONE;
            } else {
                return logWordInsertionProbability;
            }
        }


        /**
         * Generate a hashcode for an object
         *
         * @return the hashcode
         */
        @Override
        public int hashCode() {
            return 13 * gs.hashCode() + pronunciation.hashCode();
        }


        /**
         * Determines if the given object is equal to this object
         *
         * @param o the object to test
         * @return <code>true</code> if the object is equal to this
         */
        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof PronunciationState) {
                PronunciationState other = (PronunciationState) o;
                return other.gs.equals(gs) &&
                        other.pronunciation.equals(pronunciation);
            } else {
                return false;
            }
        }


        /**
         * Gets the successor states for this search graph
         *
         * @return the successor states
         */
        @Override
        public SearchStateArc[] getSuccessors() {
            SearchStateArc[] arcs = getCachedSuccessors();
            if (arcs == null) {
                arcs = getSuccessors(gs.getLC(), 0);
                cacheSuccessors(arcs);
            }
            return arcs;
        }


        /**
         * Gets the successor states for the unit and the given position and left context
         *
         * @param lc    the ID of the left context
         * @param index the position of the unit within the pronunciation
         * @return the set of sucessor arcs
         */
        SearchStateArc[] getSuccessors(int lc, int index) {
            SearchStateArc[] arcs;
            if (index == pronunciation.getUnits().length - 1) {
                if (isContextIndependentUnit(
                        pronunciation.getUnits()[index])) {
                    arcs = new SearchStateArc[1];
                    arcs[0] = new OurFullHMMSearchState(this, index, lc, ANY);
                } else {
                    int[] nextUnits = gs.getNextUnits();
                    arcs = new SearchStateArc[nextUnits.length];
                    for (int i = 0; i < arcs.length; i++) {
                        arcs[i] = new
                                OurFullHMMSearchState(this, index, lc, nextUnits[i]);
                    }
                }
            } else {
                arcs = new SearchStateArc[1];
                arcs[0] = new OurFullHMMSearchState(this, index, lc);
            }
            return arcs;
        }


        /**
         * Gets the pronunciation assocated with this state
         *
         * @return the pronunciation
         */
        @Override
        public Pronunciation getPronunciation() {
            return pronunciation;
        }


        /**
         * Determines if the given unit is a CI unit
         *
         * @param unit the unit to test
         * @return true if the unit is a context independent unit
         */
        private boolean isContextIndependentUnit(Unit unit) {
            return unit.isFiller();
        }


        /**
         * Returns a unique string representation of the state. This string is suitable (and typically used) for a label
         * for a GDL node
         *
         * @return the signature
         */
        @Override
        public String getSignature() {
            return "PS " + gs.getSignature() + '-' + pronunciation;
        }


        /**
         * Returns a string representation of this object
         *
         * @return a string representation
         */
        @Override
        public String toString() {
            return pronunciation.getWord().getSpelling();
        }


        /**
         * Returns the order of this state type among all of the search states
         *
         * @return the order
         */
        @Override
        public int getOrder() {
            return 2;
        }


        /**
         * Returns the grammar state associated with this state
         *
         * @return the grammar state
         */
        GrammarState getGrammarState() {
            return gs;
        }


        /**
         * Returns true if this WordSearchState indicates the start of a word. Returns false if this WordSearchState
         * indicates the end of a word.
         *
         * @return true if this WordSearchState indicates the start of a word, false if this WordSearchState indicates
         *         the end of a word
         */
        @Override
        public boolean isWordStart() {
            return true;
        }
    }


    /** Represents a unit (as an HMM) in the search graph */
    class OurFullHMMSearchState extends FlatSearchState implements
            UnitSearchState, ScoreProvider {

        private final PronunciationState pState;
        private final int index;
        private final boolean isLastUnitOfWord;
        
        private int numberOfTimesUsed = 0;

        /**
         * Creates a FullHMMSearchState
         *
         * @param p     the parent PronunciationState
         * @param which the index of the unit within the pronunciation
         * @param lc    the ID of the left context // können wir ignorieren
         */
        OurFullHMMSearchState(PronunciationState p, int which, int lc) {
            this(p, which, lc,
                    p.getPronunciation().getUnits()[which + 1].getBaseID());
        }


        /**
         * Creates a FullHMMSearchState
         *
         * @param p     the parent PronunciationState
         * @param which the index of the unit within the pronunciation
         * @param lc    the ID of the left context // können wir ignorieren
         * @param rc    the ID of the right context // können wir ignorieren
         */
        OurFullHMMSearchState(PronunciationState p, int which, int lc, int rc) {
            this.pState = p;
            this.index = which;
            isLastUnitOfWord =
                    which == p.getPronunciation().getUnits().length - 1;
        }


        /**
         * Determines the insertion probability based upon the type of unit
         *
         * @return the insertion probability
         */
        @Override
        public float getInsertionProbability() {
            Unit unit = pState.getPronunciation().getUnits()[index];

            if (unit.isSilence()) {
                return logSilenceInsertionProbability;
            } else if (unit.isFiller()) {
                return logFillerInsertionProbability;
            } else {
                return logUnitInsertionProbability;
            }
        }


        /**
         * Returns a string representation of this object
         *
         * @return a string representation
         */
        @Override
        public String toString() {
        	return getUnit().toString();
        }


        /**
         * Generate a hashcode for an object
         *
         * @return the hashcode
         */
        @Override
        public int hashCode() {
        	// TODO: maybe improve hash by using AtomicInteger.getNext() from a class variable
            return pState.getGrammarState().getGrammarNode().hashCode() * 29 +
                    pState.getPronunciation().hashCode() * 19 +
                    index * 7 ; //+ 43 * lc + rc;
        }


        /**
         * Determines if the given object is equal to this object
         *
         * @param o the object to test
         * @return <code>true</code> if the object is equal to this
         */
        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof OurFullHMMSearchState) {
                OurFullHMMSearchState other = (OurFullHMMSearchState) o;
                // the definition for equal for a FullHMMState:
                // Grammar Node equal
                // Pronunciation equal
                // index equal
                // rc equal

                return index == other.index && 
                		pState.getGrammarState().getGrammarNode() ==
                        other.pState.getGrammarState().getGrammarNode() &&
                        pState.getPronunciation() == other.pState.getPronunciation()
                        ;
            } else {
                return false;
            }
        }


        /**
         * Returns the unit associated with this state
         *
         * @return the unit
         */
        @Override
        public Unit getUnit() {
        	return pState.getPronunciation().getUnits()[index];
        }


        /**
         * Gets the set of successors for this state
         *
         * @return the set of successors
         */
        @Override
        public SearchStateArc[] getSuccessors() {
            SearchStateArc[] arcs = getCachedSuccessors();
            if (arcs == null) {
            	SearchStateArc[] localArcs = this.getNextArcs();
            	arcs = new SearchStateArc[localArcs.length + 1];
            	System.arraycopy(localArcs, 0, arcs, 1, localArcs.length);
            	arcs[0] = this;
                cacheSuccessors(arcs);
            }
            return arcs;
        }


        /**
         * Determines if this unit is the last unit of a word
         *
         * @return true if this unit is the last unit of a word
         */
        boolean isLastUnitOfWord() {
            return isLastUnitOfWord;
        }


        /**
         * Determines the position of the unit within the word
         *
         * @return the position of the unit within the word
         */
        HMMPosition getPosition() {
            int len = pState.getPronunciation().getUnits().length;
            if (len == 1) {
                return HMMPosition.SINGLE;
            } else if (index == 0) {
                return HMMPosition.BEGIN;
            } else if (index == len - 1) {
                return HMMPosition.END;
            } else {
                return HMMPosition.INTERNAL;
            }
        }


        /**
         * Returns the order of this state type among all of the search states
         *
         * @return the order
         */
        @Override
        public int getOrder() {
            return 3;
        }

        /**
         * Returns a unique string representation of the state. This string is suitable (and typically used) for a label
         * for a GDL node
         *
         * @return the signature
         */
        @Override
        public String getSignature() {
            return "HSS " + pState.getGrammarState().getGrammarNode() +
                    pState.getPronunciation() + index + '-'; 
        }

        /**
         * Returns the ID of the right context for this state
         *
         * @return the right context unit ID
         */
        int getRC() {
            return 0;
        }

        @Override
        public boolean isEmitting() {
            return true;
        }

        /**
         * Returns the next set of arcs after this state and all substates have been processed
         *
         * @return the next set of arcs
         */
        SearchStateArc[] getNextArcs() {
            SearchStateArc[] arcs;
            // this is the last state of the hmm
            // so check to see if we are at the end
            // of a word, if not get the next full hmm in the word
            // otherwise generate arcs to the next set of words

            if (!isLastUnitOfWord()) {
                arcs = pState.getSuccessors(0, index + 1);
            } else {
                // we are at the end of the word, so we transit to the
                // next grammar nodes
                GrammarState gs = pState.getGrammarState();
                arcs = gs.getNextGrammarStates(0, getRC());
            }
            return arcs;
        }


		@Override
		public float getScore(Data data) {
			//System.out.println("getting score");
			String name = pState.getPronunciation().getUnits()[index].getName();
			// TODO: if numberOfTimesUsed != 0 then add a penalty to the score
			numberOfTimesUsed++;

			return ((PhoneData) data).getConfusionScore(name,numberOfTimesUsed);
		}
    }



    /** The search graph that is produced by the flat linguist. */
    class DynamicFlatSearchGraph implements SearchGraph {

        /*
        * (non-Javadoc)
        *
        * @see edu.cmu.sphinx.linguist.SearchGraph#getInitialState()
        */
        @Override
        public SearchState getInitialState() {
            InitialState initialState = new InitialState();
            initialState.addArc(new GrammarState(grammar.getInitialNode()));
            // add an out-of-grammar branch if configured to do so
            return initialState;
        }


        /*
         * (non-Javadoc)
         * 
         * @see edu.cmu.sphinx.linguist.SearchGraph#getNumStateOrder()
         */
        @Override
        public int getNumStateOrder() {
            return 5;
        }
    }
}

