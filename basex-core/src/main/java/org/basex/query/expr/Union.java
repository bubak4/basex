package org.basex.query.expr;

import static org.basex.query.QueryText.*;

import org.basex.query.*;
import org.basex.query.iter.*;
import org.basex.query.util.list.*;
import org.basex.query.value.item.*;
import org.basex.query.value.node.*;
import org.basex.query.value.seq.*;
import org.basex.query.var.*;
import org.basex.util.*;
import org.basex.util.hash.*;

/**
 * Union expression.
 *
 * @author BaseX Team 2005-17, BSD License
 * @author Christian Gruen
 */
public final class Union extends Set {
  /**
   * Constructor.
   * @param info input info
   * @param exprs expressions
   */
  public Union(final InputInfo info, final Expr... exprs) {
    super(info, exprs);
  }

  @Override
  public Expr optimize(final CompileContext cc) throws QueryException {
    super.optimize(cc);

    final ExprList el = new ExprList(exprs.length);
    for(final Expr ex : exprs) {
      if(ex == Empty.SEQ) {
        // remove empty operands
        cc.info(OPTREMOVE_X_X, description(), ex);
      } else {
        el.add(ex);
      }
    }
    // no expressions: return empty sequence
    if(el.isEmpty()) return Empty.SEQ;
    // ensure that results are always sorted
    if(el.size() == 1 && iterable) return el.get(0);
    // replace expressions with optimized list
    exprs = el.finish();
    return this;
  }

  @Override
  public Expr copy(final CompileContext cc, final IntObjMap<Var> vm) {
    final Union un = new Union(info, copyAll(cc, vm, exprs));
    un.iterable = iterable;
    return copyType(un);
  }

  @Override
  public ANodeBuilder eval(final Iter[] iters, final QueryContext qc) throws QueryException {
    final ANodeBuilder list = new ANodeBuilder();
    for(final Iter ir : iters) {
      for(Item it; (it = ir.next()) != null;) {
        qc.checkStop();
        list.add(toNode(it));
      }
    }
    return list;
  }

  @Override
  protected NodeIter iter(final Iter[] iters) {
    return new SetIter(iters) {
      @Override
      public ANode next() throws QueryException {
        if(item == null) {
          final int il = iter.length;
          item = new ANode[il];
          for(int i = 0; i < il; i++) next(i);
        }

        int m = -1;
        final int il = item.length;
        for(int i = 0; i < il; i++) {
          if(item[i] == null) continue;
          final int d = m == -1 ? 1 : item[m].diff(item[i]);
          if(d == 0) {
            next(i--);
          } else if(d > 0) {
            m = i;
          }
        }
        if(m == -1) return null;

        final ANode it = item[m];
        next(m);
        return it;
      }
    };
  }

  @Override
  public boolean equals(final Object obj) {
    return this == obj || obj instanceof Union && super.equals(obj);
  }
}
