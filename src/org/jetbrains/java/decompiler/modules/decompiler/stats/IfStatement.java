/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.java.decompiler.modules.decompiler.stats;

import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.collectors.BytecodeMappingTracer;
import org.jetbrains.java.decompiler.modules.decompiler.DecHelper;
import org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor;
import org.jetbrains.java.decompiler.modules.decompiler.StatEdge;
import org.jetbrains.java.decompiler.modules.decompiler.exps.*;
import org.jetbrains.java.decompiler.util.InterpreterUtil;
import org.jetbrains.java.decompiler.util.Util;

import java.util.ArrayList;
import java.util.List;


public class IfStatement extends Statement {

  public static int IFTYPE_IF = 0;
  public static int IFTYPE_IFELSE = 1;

  public int iftype;

  // *****************************************************************************
  // private fields
  // *****************************************************************************

  private Statement ifstat;
  private Statement elsestat;

  private StatEdge ifedge;
  private StatEdge elseedge;

  private boolean negated = false;

  private boolean iffflag;

  private List<Exprent> headexprent = new ArrayList<Exprent>(); // contains IfExprent

  // *****************************************************************************
  // constructors
  // *****************************************************************************

  private IfStatement() {
    type = TYPE_IF;

    headexprent.add(null);
  }

  private IfStatement(Statement head, int regedges, Statement postst) {

    this();

    first = head;
    stats.addWithKey(head, head.id);

    List<StatEdge> lstHeadSuccs = head.getSuccessorEdges(STATEDGE_DIRECT_ALL);

    switch (regedges) {
      case 0:
        ifstat = null;
        elsestat = null;

        break;
      case 1:
        ifstat = null;
        elsestat = null;

        StatEdge edgeif = lstHeadSuccs.get(1);
        if (edgeif.getType() != StatEdge.TYPE_REGULAR) {
          post = lstHeadSuccs.get(0).getDestination();
        }
        else {
          post = edgeif.getDestination();
          negated = true;
        }
        break;
      case 2:
        elsestat = lstHeadSuccs.get(0).getDestination();
        ifstat = lstHeadSuccs.get(1).getDestination();

        List<StatEdge> lstSucc = ifstat.getSuccessorEdges(StatEdge.TYPE_REGULAR);
        List<StatEdge> lstSucc1 = elsestat.getSuccessorEdges(StatEdge.TYPE_REGULAR);

        if (ifstat.getPredecessorEdges(StatEdge.TYPE_REGULAR).size() > 1 || lstSucc.size() > 1) {
          post = ifstat;
        }
        else if (elsestat.getPredecessorEdges(StatEdge.TYPE_REGULAR).size() > 1 || lstSucc1.size() > 1) {
          post = elsestat;
        }
        else {
          if (lstSucc.size() == 0) {
            post = elsestat;
          }
          else if (lstSucc1.size() == 0) {
            post = ifstat;
          }
        }

        if (ifstat == post) {
          if (elsestat != post) {
            ifstat = elsestat;
            negated = true;
          }
          else {
            ifstat = null;
          }
          elsestat = null;
        }
        else if (elsestat == post) {
          elsestat = null;
        }
        else {
          post = postst;
        }

        if (elsestat == null) {
          regedges = 1;  // if without else
        }
    }

    ifedge = lstHeadSuccs.get(negated ? 0 : 1);
    elseedge = (regedges == 2) ? lstHeadSuccs.get(negated ? 1 : 0) : null;

    iftype = (regedges == 2) ? IFTYPE_IFELSE : IFTYPE_IF;

    if (iftype == IFTYPE_IF) {
      if (regedges == 0) {
        StatEdge edge = lstHeadSuccs.get(0);
        head.removeSuccessor(edge);
        edge.setSource(this);
        this.addSuccessor(edge);
      }
      else if (regedges == 1) {
        StatEdge edge = lstHeadSuccs.get(negated ? 1 : 0);
        head.removeSuccessor(edge);
      }
    }

    if (ifstat != null) {
      stats.addWithKey(ifstat, ifstat.id);
    }

    if (elsestat != null) {
      stats.addWithKey(elsestat, elsestat.id);
    }

    if (post == head) {
      post = this;
    }
  }


  // *****************************************************************************
  // public methods
  // *****************************************************************************

  public static Statement isHead(Statement head) {

    if (head.type == TYPE_BASICBLOCK && head.getLastBasicType() == LASTBASICTYPE_IF) {
      int regsize = head.getSuccessorEdges(StatEdge.TYPE_REGULAR).size();

      Statement p = null;

      boolean ok = (regsize < 2);
      if (!ok) {
        List<Statement> lst = new ArrayList<Statement>();
        if (DecHelper.isChoiceStatement(head, lst)) {
          p = lst.remove(0);

          for (Statement st : lst) {
            if (st.isMonitorEnter()) {
              return null;
            }
          }

          ok = DecHelper.checkStatementExceptions(lst);
        }
      }

      if (ok) {
        return new IfStatement(head, regsize, p);
      }
    }

    return null;
  }

  public String toJava(int indent, BytecodeMappingTracer tracer) {
    String indstr = InterpreterUtil.getIndentString(indent);
    StringBuilder buf = new StringBuilder();

    String new_line_separator = DecompilerContext.getNewLineSeparator();

    buf.append(ExprProcessor.listToJava(varDefinitions, indent, tracer));

    boolean bloodySpecialCases = false;
    if (first instanceof BasicBlockStatement) {
      List<Exprent> exps = first.getExprents();
      if (exps.size() != 0) {
        Exprent last = exps.get(exps.size() - 1);
        if (last instanceof AssignmentExprent) {
          AssignmentExprent assignmentExprent = (AssignmentExprent) exps.get(exps.size() - 1);
          bloodySpecialCases = true;
          if (assignmentExprent.getLeft() instanceof VarExprent) {
            VarExprent var = (VarExprent) assignmentExprent.getLeft();
            bloodySpecialCases = !var.isDefinition();
          }
        } else if (last instanceof InvocationExprent) {
          bloodySpecialCases = true;
        }
      }
    }

    if (bloodySpecialCases) {
      buf.append(Util.rtrim(first.toJava(indent, tracer))).append(new_line_separator);
    } else {
      String content = first.toJava(indent, tracer);
      buf.append(content);
      if (first instanceof BasicBlockStatement && !content.isEmpty()) {
        List<Exprent> exps = first.getExprents();
        if (exps.size() != 0) {
          Exprent e = exps.get(exps.size() - 1);
          if (!(e instanceof InvocationExprent || e instanceof FunctionExprent)) {
            buf.append(new_line_separator);
          }
        }
      }
    }

    if (isLabeled()) {
      buf.append(indstr).append("label").append(this.id).append(":").append(new_line_separator);
      tracer.incrementCurrentSourceLine();
    }

    buf.append(indstr).append(headexprent.get(0).toJava(indent, tracer)).append(" {").append(new_line_separator);
    tracer.incrementCurrentSourceLine();

    if (ifstat == null) {
      buf.append(InterpreterUtil.getIndentString(indent + 1));

      if (ifedge.explicit) {
        if (ifedge.getType() == StatEdge.TYPE_BREAK) {
          // break
          buf.append("break");
        }
        else {
          // continue
          buf.append("continue");
        }

        if (ifedge.labeled) {
          buf.append(" label").append(ifedge.closure.id);
        }
      }
      buf.append(";").append(new_line_separator);
      tracer.incrementCurrentSourceLine();
    }
    else {
      buf.append(ExprProcessor.jmpWrapper(ifstat, indent + 1, true, tracer));
    }

    boolean elseif = false;

    if (elsestat != null) {
      if (elsestat.type == Statement.TYPE_IF
          && elsestat.varDefinitions.isEmpty() && elsestat.getFirst().getExprents().isEmpty() &&
          !elsestat.isLabeled() &&
          (elsestat.getSuccessorEdges(STATEDGE_DIRECT_ALL).isEmpty()
           || !elsestat.getSuccessorEdges(STATEDGE_DIRECT_ALL).get(0).explicit)) { // else if
        String content = ExprProcessor.jmpWrapper(elsestat, indent, false, tracer);
        content = content.substring(indstr.length());

        buf.append(indstr).append("} else ");
        buf.append(content);

        elseif = true;
      }
      else {
        BytecodeMappingTracer else_tracer = new BytecodeMappingTracer(tracer.getCurrentSourceLine());
        String content = ExprProcessor.jmpWrapper(elsestat, indent + 1, false, else_tracer);

        if (content.length() > 0) {
          buf.append(indstr).append("} else {").append(new_line_separator);

          else_tracer.shiftSourceLines(1);
          tracer.setCurrentSourceLine(else_tracer.getCurrentSourceLine() + 1);
          tracer.addTracer(else_tracer);

          buf.append(content);
        }
      }
    }

    if (!elseif) {
      buf.append(indstr).append("}").append(new_line_separator);
      tracer.incrementCurrentSourceLine();
    }

    return buf.toString();
  }

  public void initExprents() {

    IfExprent ifexpr = (IfExprent)first.getExprents().remove(first.getExprents().size() - 1);

    if (negated) {
      ifexpr = (IfExprent)ifexpr.copy();
      ifexpr.negateIf();
    }

    headexprent.set(0, ifexpr);
  }

  public List<Object> getSequentialObjects() {

    List<Object> lst = new ArrayList<Object>(stats);
    lst.add(1, headexprent.get(0));

    return lst;
  }

  public void replaceExprent(Exprent oldexpr, Exprent newexpr) {
    if (headexprent.get(0) == oldexpr) {
      headexprent.set(0, newexpr);
    }
  }

  public void replaceStatement(Statement oldstat, Statement newstat) {

    super.replaceStatement(oldstat, newstat);

    if (ifstat == oldstat) {
      ifstat = newstat;
    }

    if (elsestat == oldstat) {
      elsestat = newstat;
    }

    List<StatEdge> lstSuccs = first.getSuccessorEdges(STATEDGE_DIRECT_ALL);

    if (iftype == IFTYPE_IF) {
      ifedge = lstSuccs.get(0);
      elseedge = null;
    }
    else {
      StatEdge edge0 = lstSuccs.get(0);
      StatEdge edge1 = lstSuccs.get(1);
      if (edge0.getDestination() == ifstat) {
        ifedge = edge0;
        elseedge = edge1;
      }
      else {
        ifedge = edge1;
        elseedge = edge0;
      }
    }
  }

  public Statement getSimpleCopy() {

    IfStatement is = new IfStatement();
    is.iftype = this.iftype;
    is.negated = this.negated;
    is.iffflag = this.iffflag;

    return is;
  }

  public void initSimpleCopy() {

    first = stats.get(0);

    List<StatEdge> lstSuccs = first.getSuccessorEdges(STATEDGE_DIRECT_ALL);
    ifedge = lstSuccs.get((iftype == IFTYPE_IF || negated) ? 0 : 1);
    if (stats.size() > 1) {
      ifstat = stats.get(1);
    }

    if (iftype == IFTYPE_IFELSE) {
      elseedge = lstSuccs.get(negated ? 1 : 0);
      elsestat = stats.get(2);
    }
  }

  // *****************************************************************************
  // getter and setter methods
  // *****************************************************************************

  public Statement getElsestat() {
    return elsestat;
  }

  public void setElsestat(Statement elsestat) {
    this.elsestat = elsestat;
  }

  public Statement getIfstat() {
    return ifstat;
  }

  public void setIfstat(Statement ifstat) {
    this.ifstat = ifstat;
  }

  public boolean isNegated() {
    return negated;
  }

  public void setNegated(boolean negated) {
    this.negated = negated;
  }

  public List<Exprent> getHeadexprentList() {
    return headexprent;
  }

  public IfExprent getHeadexprent() {
    return (IfExprent)headexprent.get(0);
  }

  public boolean isIffflag() {
    return iffflag;
  }

  public void setIffflag(boolean iffflag) {
    this.iffflag = iffflag;
  }

  public void setElseEdge(StatEdge elseedge) {
    this.elseedge = elseedge;
  }

  public void setIfEdge(StatEdge ifedge) {
    this.ifedge = ifedge;
  }

  public StatEdge getIfEdge() {
    return ifedge;
  }

  public StatEdge getElseEdge() {
    return elseedge;
  }
}
