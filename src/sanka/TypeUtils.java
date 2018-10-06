package sanka;

import java.util.ArrayList;
import java.util.List;

public class TypeUtils {
    /**
     * Determine if an expression can be promoted to a type.
     *
     * @param type is an empty slot with a well defined type, such as a declared
     * variable, or an object's field, or a method parameter, or a return type.
     *
     * @param expr is the expression to be stored in the slot, such as the RHS of
     * an assignment, or an argument to pass to a method, or a value to return.
     * If expr must be converted or promoted, then modify it as necessary.
     *
     * @return true if the expression's type is equal to the given type (after
     * optional conversion or promotion)
     */
    static boolean isCompatible(TypeDefinition type, ExpressionDefinition expr) {
        if (expr.type == null) {
            // We've already printed an error. We won't continue to the next pass.
            // Finish this pass.
            return true;
        }
        if (expr.type.isNullType()) {
            // Match null and non-primitive classes and arrays and maps.
            return type.isNullType() || !type.isPrimitiveType;
        }
        if (expr.type.equals(type)) {
            return true;
        }
        if (expr.type.arrayOf != null) {
            // With arrays and maps, there's no promotion.
            // If the expression is an array of ints, then the parameter must be
            // an array of ints. An array of shorts is not a match.
            // That's why this function is not recursive.
            return false;
        }
        if (expr.type.isNumericType()) {
            return type.isNumericType() && isCompatibleNumeric(type, expr.type);
        }
        if (expr.type.isPrimitiveType) {
            return false;
        }
        Environment env = Environment.getInstance();
        ClassDefinition exprClass = env.getClassDefinition(expr.type);
        ClassDefinition typeClass = env.getClassDefinition(type);
        if (exprClass == null || typeClass == null || !typeClass.isInterface) {
            return false;
        }
        // Decide if exprClass implements the interface.
        // Should we keep a cache of what classes have matched interfaces?
        return false;
    }

    /**
     * Determine if an expression can be promoted to a numeric type.
     *
     * @return true if the expression can be promoted to the type.
     */
    static boolean isCompatibleNumeric(TypeDefinition type, TypeDefinition exprType) {
        if (exprType.equals(TypeDefinition.BYTE_TYPE)) {
            return true;
        }
        if (exprType.equals(TypeDefinition.SHORT_TYPE)) {
            return !(type.equals(TypeDefinition.BYTE_TYPE));
        }
        if (exprType.equals(TypeDefinition.INT_TYPE)) {
            return !(type.equals(TypeDefinition.BYTE_TYPE) ||
                    type.equals(TypeDefinition.SHORT_TYPE));
        }
        // long, float, and double can be converted to themselves and double.
        return type.equals(exprType) || type.equals(TypeDefinition.DOUBLE_TYPE);
    }

    static void foo(ClassDefinition interfaceDef, ClassDefinition classDef) {
        List<String> failureList = new ArrayList<>();
        for (MethodDefinition method : interfaceDef.methodList) {
            MethodDefinition implementer = classDef.getMethod(method.name);
            if (implementer == null || implementer.isPrivate ||
                    implementer.isStatic != method.isStatic ||
                    implementer.parameters.size() != method.parameters.size()) {
                failureList.add(method.name);
                continue;
            }
            for (int idx = 0; idx < implementer.parameters.size(); idx++) {

            }
        }
    }
}
