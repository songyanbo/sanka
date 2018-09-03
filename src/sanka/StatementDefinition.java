package sanka;

import java.util.List;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import sanka.ClassDefinition.FieldDefinition;
import sanka.antlr4.SankaLexer;
import sanka.antlr4.SankaParser.AssignableContext;
import sanka.antlr4.SankaParser.ExpressionContext;
import sanka.antlr4.SankaParser.ForControlContext;
import sanka.antlr4.SankaParser.ForIncrementContext;
import sanka.antlr4.SankaParser.ForInitContext;
import sanka.antlr4.SankaParser.StatementContext;
import sanka.antlr4.SankaParser.VariableAssignmentContext;
import sanka.antlr4.SankaParser.VariableDeclarationContext;

public class StatementDefinition {

    StatementContext ctx;
    int statementType;
    ExpressionDefinition lhsExpression;
    String name;
    ExpressionDefinition expression;
    StatementDefinition[] block;
    StatementDefinition statement1;
    StatementDefinition statement2;

    void parse(StatementContext ctx) {
        this.ctx = ctx;
    }

    void evaluate(StatementContext ctx) {
        this.ctx = ctx;
        evaluate();
    }

    void evaluate() {
        Environment env = Environment.getInstance();
        this.statementType = this.ctx.getStart().getType();
        if (this.ctx.constDeclaration() != null) {
            this.statementType = SankaLexer.CONST;
            env.printError(this.ctx, "const support not implemented");
            return;
        }
        if (this.ctx.variableDeclaration() != null) {
            evaluateVariableDeclaration(this.ctx.variableDeclaration());
            return;
        }
        if (this.ctx.variableAssignment() != null) {
            evaluateVariableAssignment(this.ctx.variableAssignment());
            return;
        }
        if (this.ctx.blockAsStatement() != null) {
            this.statementType = SankaLexer.LBRACE;
            List<StatementContext> statements = this.ctx.blockAsStatement().block().statement();
            this.block = new StatementDefinition[statements.size()];
            for (int idx = 0; idx < this.block.length; idx++) {
                this.block[idx] = new StatementDefinition();
                this.block[idx].evaluate(statements.get(idx));
            }
            return;
        }
        if (this.ctx.getChildCount() == 2) {
            ParseTree child0 = this.ctx.getChild(0);
            ParseTree child1 = this.ctx.getChild(1);
            if (child0 instanceof ExpressionContext && child1 instanceof TerminalNode) {
                evaluateExpressionStatement((ExpressionContext) child0);
                if (!child1.getText().equals(";")) {
                    env.printError(this.ctx, "unrecognized statement");
                }
                return;
            }
        }
        switch (this.statementType) {
        case SankaLexer.IF:
            evaluateBooleanExpression(this.ctx.parExpression().expression());
            this.statement1 = new StatementDefinition();
            this.statement1.evaluate(this.ctx.statement(0));
            if (this.ctx.statement(1) != null) {
                this.statement2 = new StatementDefinition();
                this.statement2.evaluate(this.ctx.statement(1));
            }
            return;
        case SankaLexer.WHILE:
            evaluateBooleanExpression(this.ctx.parExpression().expression());
            this.statement1 = new StatementDefinition();
            this.statement1.evaluate(this.ctx.statement(0));
            return;
        case SankaLexer.FOR:
            evaluateFor(this.ctx.forControl(), this.ctx.statement(0));
            return;
        case SankaLexer.SWITCH:
            // TODO
            env.printError(this.ctx, "switch support not implemented");
            return;
        case SankaLexer.RETURN:
            this.expression = new ExpressionDefinition();
            this.expression.evaluate(this.ctx.expression());
            if (this.expression.type != null) {
                TypeDefinition desired = env.currentMethod.returnType;
                if (!desired.isCompatible(this.expression.type)) {
                    env.printError(this.ctx, "incompatible types: " + this.expression.type +
                            " cannot be converted to " + desired);
                }
            }
            return;
        case SankaLexer.BREAK:
        case SankaLexer.CONTINUE:
        case SankaLexer.SEMI:
            return;
        }
        env.printError(this.ctx, "unrecognized statement");
    }

    /**
     * Evaluate a variable declaration statement.
     */
    void evaluateVariableDeclaration(VariableDeclarationContext vc) {
        Environment env = Environment.getInstance();
        this.statementType = SankaLexer.VAR;
        // TODO if variable is static then print error
        // TODO if variable is inline then note check RHS restriction
        this.name = vc.Identifier().getText();
        if (env.symbolTable.containsKey(this.name)) {
            env.printError(vc, "variable " + this.name + " declared twice");
        }
        if (vc.expression() == null) {
            env.symbolTable.put(this.name, null);
        } else {
            this.expression = new ExpressionDefinition();
            this.expression.evaluate(vc.expression());
            env.symbolTable.put(this.name, this.expression.type);
        }
    }

    /**
     * Evaluate a variable assignment statement.
     */
    void evaluateVariableAssignment(VariableAssignmentContext assignment) {
        Environment env = Environment.getInstance();
        String operator = ((TerminalNode) assignment.getChild(1)).getSymbol().getText();
        if (operator.equals("=")) {
            this.statementType = SankaLexer.EQUAL;
        } else if (operator.equals("++")) {
            this.statementType = SankaLexer.INC;
        } else if (operator.equals("--")) {
            this.statementType = SankaLexer.DEC;
        } else {
            env.printError(assignment, "unrecognized variable assignment statement");
        }
        if (assignment.expression() != null) {
            this.expression = new ExpressionDefinition();
            this.expression.evaluate(assignment.expression());
            if (this.expression.type == null) {
                return;
            }
        }
        AssignableContext assignable = assignment.assignable();
        if (assignable.Identifier() == null) {
            // The LHS is made of two expressions: The array and index.
            // Read them into a single ExpressionDefinition.
            this.expression = new ExpressionDefinition();
            this.expression.evaluateArrayAccess(assignable.expression(0),
                    assignable.expression(1));
            return;
        }
        this.name = assignable.Identifier().getText();
        if (assignable.expression(0) != null) {
            this.lhsExpression = new ExpressionDefinition();
            this.lhsExpression.evaluate(assignable.expression(0));
            TypeDefinition type = this.lhsExpression.type;
            if (type == null) {
                return;
            }
            if (type.arrayCount > 0) {
                env.printError(assignment,
                        "array type " + type + ": cannot modify fields (" + this.name + ")");
                return;
            }
            if (type.primitiveType != null) {
                env.printError(assignment,
                        "primitive type " + type + ": cannot modify fields (" + this.name + ")");
                return;
            }
            // TODO if type == class then assign static field
            ClassDefinition classdef = env.getClassDefinition(type);
            if (classdef == null) {
                env.printError(assignment, "type " + type + " undefined");
                return;
            }
            FieldDefinition fielddef = classdef.fieldMap.get(this.name);
            if (fielddef == null) {
                env.printError(assignment, "type " + type + " does not have field " + this.name);
                return;
            }
            if (fielddef.isPrivate && classdef != env.currentClass) {
                env.printError(assignment, "type " + type + " field " + this.name +
                        " is private");
            }
            if (this.statementType == SankaLexer.EQUAL) {
                // TODO if field is inline then note check RHS restriction
                if (!fielddef.type.isCompatible(this.expression.type)) {
                    env.printError(assignment, "incompatible types: " + this.expression.type +
                            " cannot be converted to " + fielddef.type);
                    return;
                }
            } else {
                if (!fielddef.type.isIntegralType()) {
                    env.printError(assignment, "incompatible types: " + fielddef.type +
                            " cannot be incremented");
                    return;
                }
            }
            // Ok, we allow this field assignment.
            return;
        }
        if (!env.symbolTable.containsKey(this.name)) {
            env.printError(assignment, "variable " + this.name + " undefined");
            return;
        }
        TypeDefinition varType = env.symbolTable.get(this.name);
        if (varType == null) {
            if (this.statementType != SankaLexer.EQUAL) {
                env.printError(assignment, "variable " + this.name + " uninitialized");
                return;
            }
            env.symbolTable.put(this.name, this.expression.type);
            return;
        }
        if (this.statementType == SankaLexer.EQUAL) {
            if (!varType.isCompatible(this.expression.type)) {
                env.printError(assignment, "incompatible types: " + this.expression.type +
                        " cannot be converted to " + varType);
                return;
            }
        } else {
            if (!varType.isIntegralType()) {
                env.printError(assignment, "incompatible types: " + varType +
                        " cannot be incremented");
                return;
            }
        }
        // Ok, we allow this variable assignment.
        return;
    }

    /**
     * Evaluate an expression as a standalone statement. Use BOOLEAN as the statement type simply because
     * it's a constant that's available for usage.
     */
    void evaluateExpressionStatement(ExpressionContext expr) {
        this.statementType = SankaLexer.BOOLEAN;
        this.expression = new ExpressionDefinition();
        this.expression.evaluate(expr);
    }

    /**
     * Check the clause of an "if", "while", or "for" statement.
     */
    void evaluateBooleanExpression(ExpressionContext exprCtx) {
        Environment env = Environment.getInstance();
        this.expression = new ExpressionDefinition();
        this.expression.evaluate(exprCtx);
        if (this.expression.type != null && !this.expression.type.isBooleanType()) {
            env.printError(exprCtx, "incompatible types: " + this.expression.type +
                    " cannot be converted to boolean");
        }
    }

    /**
     * Evaluate a "for" statement.
     */
    void evaluateFor(ForControlContext forControl, StatementContext statementCtx) {
        if (forControl.enhancedForControl() != null) {
            // TODO enhanced for
            return;
        }
        this.block = new StatementDefinition[3];
        ForInitContext forInit = forControl.forInit();
        if (forInit != null) {
            this.block[0] = new StatementDefinition();
            if (forInit.variableDeclaration() != null) {
                this.block[0].evaluateVariableDeclaration(forInit.variableDeclaration());
            }
            else if (forInit.variableAssignment() != null) {
                this.block[0].evaluateVariableAssignment(forInit.variableAssignment());
            }
            else if (forInit.expression() != null) {
                this.block[0].evaluateExpressionStatement(forInit.expression());
            }
        }
        evaluateBooleanExpression(forControl.expression());
        ForIncrementContext forIncrement = forControl.forIncrement();
        if (forIncrement != null) {
            this.block[1] = new StatementDefinition();
            if (forIncrement.variableAssignment() != null) {
                this.block[1].evaluateVariableAssignment(forIncrement.variableAssignment());
            }
            else if (forIncrement.expression() != null) {
                this.block[1].evaluateExpressionStatement(forIncrement.expression());
            }
        }
        this.block[2] = new StatementDefinition();
        this.block[2].evaluate(statementCtx);
    }

    void translate() {
    }
}
