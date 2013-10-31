package org.basex.query.expr;

import static org.basex.query.QueryText.*;

import org.basex.data.*;
import org.basex.index.*;
import org.basex.index.query.*;
import org.basex.query.*;
import org.basex.query.func.*;
import org.basex.query.iter.*;
import org.basex.query.util.*;
import org.basex.query.value.item.*;
import org.basex.query.value.node.*;
import org.basex.query.var.*;
import org.basex.util.*;
import org.basex.util.hash.*;

/**
 * This index class retrieves ranges from a value index.
 *
 * @author BaseX Team 2005-13, BSD License
 * @author Christian Gruen
 */
public final class StringRangeAccess extends IndexAccess {
  /** Index token. */
  private final StringRange sr;

  /**
   * Constructor.
   * @param ii input info
   * @param t index reference
   * @param ic index context
   */
  public StringRangeAccess(final InputInfo ii, final StringRange t,
      final IndexContext ic) {
    super(ic, ii);
    sr = t;
  }

  @Override
  public AxisIter iter(final QueryContext ctx) {
    final boolean text = sr.type == IndexType.TEXT;
    final byte kind = text ? Data.TEXT : Data.ATTR;
    final Data data = ictx.data;
    final int ml = data.meta.maxlen;
    final IndexIterator ii = sr.min.length <= ml && sr.max.length <= ml &&
        (text ? data.meta.textindex : data.meta.attrindex) ? data.iter(sr) : scan();

    return new AxisIter() {
      @Override
      public ANode next() {
        return ii.more() ? new DBNode(data, ii.pre(), kind) : null;
      }
    };
  }

  /**
   * Returns scan-based iterator.
   * @return node iterator
   */
  private IndexIterator scan() {
    return new IndexIterator() {
      final boolean text = sr.type == IndexType.TEXT;
      final byte kind = text ? Data.TEXT : Data.ATTR;
      int pre = -1;

      @Override
      public int pre() {
        return pre;
      }
      @Override
      public boolean more() {
        final Data data = ictx.data;
        while(++pre < data.meta.size) {
          if(data.kind(pre) != kind) continue;
          final byte[] t = data.text(pre, text);
          final int mn = Token.diff(t, sr.min);
          final int mx = Token.diff(t, sr.max);
          if(mn >= (sr.mni ? 0 : 1) && mx <= (sr.mxi ? 0 : 1)) return true;
        }
        return false;
      }
    };
  }

  @Override
  public Expr copy(final QueryContext ctx, final VarScope scp, final IntObjMap<Var> vs) {
    return new StringRangeAccess(info, sr, ictx);
  }

  @Override
  public void plan(final FElem plan) {
    addPlan(plan, planElem(DATA, ictx.data.meta.name,
        MIN, sr.min, MAX, sr.max, TYP, sr.type));
  }

  @Override
  public String toString() {
    return (sr.type == IndexType.TEXT ? Function._DB_TEXT_RANGE :
      Function._DB_ATTRIBUTE_RANGE).get(null, info, Str.get(ictx.data.meta.name),
          Str.get(sr.min), Str.get(sr.max)).toString();
  }
}
