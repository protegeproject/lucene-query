package edu.stanford.smi.protege.query.querytypes.impl;

import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.query.querytypes.BoundableQuery;
import edu.stanford.smi.protege.query.querytypes.QueryVisitor;
import edu.stanford.smi.protege.query.querytypes.VisitableQuery;
import edu.stanford.smi.protege.query.ui.QueryUtil;

public class LuceneBrowserTextSearch implements VisitableQuery, BoundableQuery {
    private static final long serialVersionUID = 8346228795720644828L;
    private String text;
    private int maxMatches = KnowledgeBase.UNLIMITED_MATCHES;

    public LuceneBrowserTextSearch(String text) {
        this.text = text;
    }  

    public String getText() {
        return text;
    }
    
    public int getMaxMatches() {
        return maxMatches;
    }

    public void setMaxMatches(int maxMatches) {
        this.maxMatches = maxMatches;
    }
    
    public BoundableQuery shallowClone() {
        return new LuceneBrowserTextSearch(text);
    }

    public void accept(QueryVisitor visitor) {
        visitor.visit(this);
    }
    
    public void localize(KnowledgeBase kb) {
        ;   
    }

    public String toString()  {
        return toString(0);
    }

    public String toString(int indent) {
        StringBuffer indentStr = QueryUtil.getIndentString(indent);
        StringBuffer buffer = new StringBuffer();
        buffer.append(indentStr);
        buffer.append("  [Lucene browser text search (");
        buffer.append(text);
        buffer.append(")]");
        return buffer.toString();
    }


}
