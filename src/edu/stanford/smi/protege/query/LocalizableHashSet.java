package edu.stanford.smi.protege.query;

import java.util.HashSet;

import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Localizable;
import edu.stanford.smi.protege.util.LocalizeUtils;

public class LocalizableHashSet<X> extends HashSet<X> implements Localizable {

  public void localize(KnowledgeBase kb) {
    for (X x : this) {
      LocalizeUtils.localize(x, kb);
    }
  }
  
}