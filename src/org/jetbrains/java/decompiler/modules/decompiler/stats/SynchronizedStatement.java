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
import org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor;
import org.jetbrains.java.decompiler.modules.decompiler.SequenceHelper;
import org.jetbrains.java.decompiler.modules.decompiler.StatEdge;
import org.jetbrains.java.decompiler.modules.decompiler.exps.*;
import org.jetbrains.java.decompiler.util.InterpreterUtil;

import java.util.ArrayList;
import java.util.List;


public class SynchronizedStatement extends Statement {

  private Statement body;

  private List<Exprent> headexprent = new ArrayList<Exprent>();

  // *****************************************************************************
  // constructors
  // *****************************************************************************

  public SynchronizedStatement() {
    type = TYPE_SYNCRONIZED;

    headexprent.add(null);
  }

  public SynchronizedStatement(Statement head, Statement body, Statement exc) {

    this();

    first = head;
    stats.addWithKey(head, head.id);

    this.body = body;
    stats.addWithKey(body, body.id);

    stats.addWithKey(exc, exc.id);

    List<StatEdge> lstSuccs = body.getSuccessorEdges(STATEDGE_DIRECT_ALL);
    if (!lstSuccs.isEmpty()) {
      StatEdge edge = lstSuccs.get(0);
      if (edge.getType() == StatEdge.TYPE_REGULAR) {
        post = edge.getDestination();
      }
    }
  }


  // *****************************************************************************
  // public methods
  // *****************************************************************************

  public String toJava(int indent, BytecodeMappingTracer tracer) {
    String indstr = InterpreterUtil.getIndentString(indent);

    String new_line_separator = DecompilerContext.getNewLineSeparator();

    StringBuilder buf = new StringBuilder();

    String content = ExprProcessor.listToJava(varDefinitions, indent, tracer);
    buf.append(content);
    if (!content.isEmpty()) {
      buf.append(new_line_separator);
    }

    content = first.toJava(indent, tracer);
    buf.append(content);
    if (first instanceof BasicBlockStatement && !content.isEmpty()) {
      List<Exprent> exps = first.getExprents();
      if (exps.size() != 0) {
        Exprent e = exps.get(exps.size() - 1);
        if (!(e instanceof InvocationExprent
                || e instanceof FunctionExprent
                || (e instanceof AssignmentExprent && !(((AssignmentExprent) e).getLeft() instanceof VarExprent && ((VarExprent) ((AssignmentExprent) e).getLeft()).isDefinition())))) {
          buf.append(new_line_separator);
        }
      }
    }

    if (isLabeled()) {
      buf.append(indstr).append("label").append(this.id).append(":").append(new_line_separator);
      tracer.incrementCurrentSourceLine();
    }

    buf.append(indstr).append(headexprent.get(0).toJava(indent, tracer)).append(" {").append(new_line_separator);
    tracer.incrementCurrentSourceLine();

    buf.append(ExprProcessor.jmpWrapper(body, indent + 1, true, tracer));
    tracer.incrementCurrentSourceLine();

    buf.append(indstr).append("}").append(new_line_separator);
    tracer.incrementCurrentSourceLine();

    return buf.toString();
  }

  public void initExprents() {
    headexprent.set(0, first.getExprents().remove(first.getExprents().size() - 1));
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

    if (body == oldstat) {
      body = newstat;
    }

    super.replaceStatement(oldstat, newstat);
  }

  public void removeExc() {
    Statement exc = stats.get(2);
    SequenceHelper.destroyStatementContent(exc, true);

    stats.removeWithKey(exc.id);
  }

  public Statement getSimpleCopy() {
    return new SynchronizedStatement();
  }

  public void initSimpleCopy() {
    first = stats.get(0);
    body = stats.get(1);
  }

  // *****************************************************************************
  // getter and setter methods
  // *****************************************************************************

  public Statement getBody() {
    return body;
  }

  public void setBody(Statement body) {
    this.body = body;
  }

  public List<Exprent> getHeadexprentList() {
    return headexprent;
  }

  public Exprent getHeadexprent() {
    return headexprent.get(0);
  }
}
