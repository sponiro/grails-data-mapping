package org.grails.datastore.gorm.neo4j;

import org.grails.datastore.mapping.model.types.Association;

import java.util.*;

/**
 * A builder for Cypher queries
 *
 * @since 3.0
 * @author Stefan
 * @author Graeme Rocher
 *
 */
public class CypherBuilder {

    public final static String TYPE = "type";
    public final static String END = "end";
    public final static String START = "start";
    public static final String IDENTIFIER = "__id__";
    public static final String PROPS = "props";
    public static final String RELATED = "related";
    public static final String WHERE = " WHERE ";
    public static final String RETURN = " RETURN ";
    public static final String COMMAND_SEPARATOR = ", ";
    public static final String DEFAULT_RETURN_TYPES = "n as data\n";
    public static final String DEFAULT_RETURN_STATEMENT = RETURN + DEFAULT_RETURN_TYPES;
    public static final String NEW_LINE = " \n";
    public static final String START_MATCH = "MATCH (n";
    public static final String SPACE = " ";
    public static final String OPTIONAL_MATCH = "OPTIONAL MATCH";
    public static final String CYPHER_CREATE = "CREATE ";
    public static final String CYPHER_MATCH_ID = "MATCH (n%s) WHERE n.__id__={id}";
    public static final String NODE_LABELS = "labels";
    public static final String NODE_DATA = "data";
    public final static String NODE_VAR = "n";
    public static final String DELETE = "\n DELETE ";


    private String forLabels;
    private Set<String> matches = new HashSet<String>();
    private Set<String> optionalMatches = new HashSet<String>();
    private String conditions;
    private String orderAndLimits;
    private List<String> returnColumns = new ArrayList<String>();
    private List<String> deleteColumns = new ArrayList<String>();
    private Map<String, Object> params = new LinkedHashMap<String, Object>();

    public CypherBuilder(String forLabels) {
        this.forLabels = forLabels;
    }

    public void addMatch(String match) {
        matches.add(match);
    }

    /**
     * Optional matches are added to do joins for relationships
     *
     * @see <a href="http://neo4j.com/docs/stable/query-optional-match.html">http://neo4j.com/docs/stable/query-optional-match.html</a>
     *
     * @param match The optional match
     */
    public void addOptionalMatch(String match) {
        optionalMatches.add(match);
    }

    public int getNextMatchNumber() {
        return matches.size();
    }

    public void setConditions(String conditions) {
        this.conditions = conditions;
    }

    public void setOrderAndLimits(String orderAndLimits) {
        this.orderAndLimits = orderAndLimits;
    }

    public int addParam(Object value) {
        params.put(String.valueOf(params.size() + 1), value);
        return params.size();
    }

    /**
     *
     * @param position first element is 1
     * @param value
     */
    public void replaceParamAt(int position, Object value) {
        params.put(String.valueOf(position), value);
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void addReturnColumn(String returnColumn) {
        returnColumns.add(returnColumn);
    }

    public void addDeleteColumn(String deleteColumn) {
        deleteColumns.add(deleteColumn);
    }

    public String build() {
        StringBuilder cypher = new StringBuilder();
        cypher.append(START_MATCH).append(forLabels).append(")");

        for (String m : matches) {
            cypher.append(COMMAND_SEPARATOR).append(m);
        }


        if ((conditions!=null) && (!conditions.isEmpty())) {
            cypher.append(WHERE).append(conditions);
        }

        if(!optionalMatches.isEmpty()) {
            for (String m : optionalMatches) {
                cypher.append(NEW_LINE)
                      .append(OPTIONAL_MATCH)
                      .append(m);

            }
        }

        if (returnColumns.isEmpty()) {
            if(deleteColumns.isEmpty()) {
                cypher.append(DEFAULT_RETURN_STATEMENT);
                if (orderAndLimits!=null) {
                    cypher.append(orderAndLimits).append(NEW_LINE);
                }
            }
            else {
                cypher.append(DELETE);
                Iterator<String> iter = deleteColumns.iterator();   // same as Collection.join(String separator)
                if (iter.hasNext()) {
                    cypher.append(iter.next());
                    while (iter.hasNext()) {
                        cypher.append(COMMAND_SEPARATOR).append(iter.next());
                    }
                }
            }
        } else {
            cypher.append(RETURN);
            Iterator<String> iter = returnColumns.iterator();   // same as Collection.join(String separator)
            if (iter.hasNext()) {
                cypher.append(iter.next());
                while (iter.hasNext()) {
                    cypher.append(COMMAND_SEPARATOR).append(iter.next());
                }
            }
            if (orderAndLimits!=null) {
                cypher.append(SPACE);
                cypher.append(orderAndLimits);
            }
        }

        return cypher.toString();
    }

    public static String findRelationshipEndpointIdsFor(Association association) {
        String relType = RelationshipUtils.relationshipTypeUsedFor(association);
        boolean reversed = RelationshipUtils.useReversedMappingFor(association);

        StringBuilder sb = new StringBuilder();
        GraphPersistentEntity graphPersistentEntity = (GraphPersistentEntity) (association.getOwner());
        String labels = graphPersistentEntity.getLabelsAsString();
        sb.append("MATCH (me").append(labels).append(" {__id__:{1}})");
        if (reversed) {
            sb.append("<");
        }
        sb.append("-[:").append(relType).append("]-");
        if (!reversed) {
            sb.append(">");
        }
        sb.append("(other) RETURN other.__id__ as id");
        return sb.toString();
    }

}