package com.openosrs.injector;

import com.openosrs.injector.injection.InjectData;
import static com.openosrs.injector.rsapi.RSApi.*;
import static com.openosrs.injector.rsapi.RSApi.API_BASE;
import com.openosrs.injector.rsapi.RSApiClass;
import com.openosrs.injector.rsapi.RSApiMethod;
import java.util.List;
import java.util.stream.Collectors;
import net.runelite.asm.Annotated;
import net.runelite.asm.ClassFile;
import net.runelite.asm.ClassGroup;
import net.runelite.asm.Field;
import net.runelite.asm.Method;
import net.runelite.asm.Named;
import net.runelite.asm.Type;
import net.runelite.asm.attributes.annotation.Annotation;
import net.runelite.asm.attributes.code.Instruction;
import net.runelite.asm.attributes.code.InstructionType;
import net.runelite.asm.attributes.code.Instructions;
import net.runelite.asm.attributes.code.instructions.ALoad;
import net.runelite.asm.attributes.code.instructions.DLoad;
import net.runelite.asm.attributes.code.instructions.FLoad;
import net.runelite.asm.attributes.code.instructions.ILoad;
import net.runelite.asm.attributes.code.instructions.InvokeStatic;
import net.runelite.asm.attributes.code.instructions.InvokeVirtual;
import net.runelite.asm.attributes.code.instructions.LLoad;
import net.runelite.asm.attributes.code.instructions.Return;
import net.runelite.asm.attributes.code.instructions.VReturn;
import net.runelite.asm.pool.Class;
import net.runelite.asm.signature.Signature;
import net.runelite.deob.DeobAnnotations;

public interface InjectUtil
{
	/**
	 * Finds a static method in deob and converts it to ob
	 *
	 * @param data InjectData instance
	 * @param name The name of the method you want to find
	 * @return The obfuscated version of the found method
	 */
	static Method findStaticMethod(InjectData data, String name) throws Injexception
	{
		return findStaticMethod(data, name, null, null);
	}

	/**
	 * Finds a static method in deob and converts it to ob
	 *
	 * @param data InjectData instance
	 * @param name The name of the method you want to find
	 * @param classHint The name of the class you expect the method to be in, or null
	 * @param sig The signature the method has in deob, or null
	 *
	 * @return The obfuscated version of the found method
	 */
	static Method findStaticMethod(InjectData data, String name, String classHint, Signature sig) throws Injexception
	{
		final ClassGroup deob = data.getDeobfuscated();
		Method method;

		if (classHint != null)
		{
			ClassFile clazz = findClassOrThrow(deob, classHint);

			if (sig == null)
			{
				method = clazz.findStaticMethod(name);
			}
			else
			{
				method = clazz.findStaticMethod(name, sig);
			}

			if (method != null)
				return data.toVanilla(method);
		}

		for (ClassFile clazz : deob)
		{
			if (sig == null)
			{
				method = clazz.findStaticMethod(name);
			}
			else
			{
				method = clazz.findStaticMethod(name, sig);
			}

			if (method != null)
				return data.toVanilla(method);
		}

		throw new Injexception("Static method " + name + " doesn't exist");
	}

	static ClassFile findClassOrThrow(ClassGroup group, String name) throws Injexception
	{
		ClassFile clazz = group.findClass(name);
		if (clazz == null)
			throw new Injexception("Hint class " + name + " doesn't exist");

		return clazz;
	}

	/**
	 * Finds a static method in deob and converts it to ob
	 *
	 * @param data InjectData instance
	 * @param pool Pool method of the method you want
	 *
	 * @return The obfuscated version of the found method
	 */
	static Method findStaticMethod(InjectData data, net.runelite.asm.pool.Method pool) throws Injexception
	{
		return findStaticMethod(data, pool.getName(), pool.getClazz().getName(), pool.getType());
	}

	static Method findMethodWithArgs(InjectData data, String name, String hintClass, Signature sig) throws Injexception
	{
		final ClassGroup deob = data.getDeobfuscated();
		if (hintClass != null)
		{
			ClassFile clazz = findClassOrThrow(deob, hintClass);
			Method method = clazz.findStaticMethod(name);

			if (method != null && argsMatch(sig, method.getDescriptor()))
				return data.toVanilla(method);
		}

		for (ClassFile c : deob)
			for (Method m : c.getMethods())
				if (m.getName().equals(name) && argsMatch(sig, m.getDescriptor()))
					return data.toVanilla(m);

		throw new Injexception("Method called " + name + " with args matching " + sig + " doesn't exist");
	}

	static Method findMethodWithArgsDeep(InjectData data, ClassFile clazz, String name, Signature sig) throws Injexception
	{
		do
			for (Method m : clazz.getMethods())
				if (m.getName().equals(name) && argsMatch(sig, m.getDescriptor()))
					return data.toVanilla(m);
		while ((clazz = clazz.getParent()) != null);

		throw new Injexception("Method called " + name + " with args matching " + sig + " doesn't exist");
	}

	/**
	 * Fail-fast implementation of ClassGroup.findStaticMethod
	 */
	static Method findStaticMethod(ClassGroup group, String name, Signature type) throws Injexception
	{
		Method m = group.findStaticMethod(name, type);
		if (m == null)
		{
			throw new Injexception(String.format("Method %s couldn't be found", name + type.toString()));
		}
		return m;
	}

	/**
	 * Fail-fast implementation of ClassFile.findMethodDeep
	 */
	static Method findMethodDeep(ClassFile clazz, String name, Signature type) throws Injexception
	{
		Method m = clazz.findMethodDeep(name, type);
		if (m == null)
		{
			throw new Injexception(String.format("Method %s couldn't be found", name + type.toString()));
		}
		return m;
	}

	/**
	 * Fail-fast implementation of ClassGroup.findStaticField
	 *
	 * well...
	 */
	static Field findStaticField(ClassGroup group, String name) throws Injexception
	{
		for (ClassFile clazz : group)
		{
			Field f = clazz.findField(name);
			if (f != null && f.isStatic())
			{
				return f;
			}
		}
		throw new Injexception("Couldn't find static field " + name);
	}

	/**
	 * Finds a static field in deob and converts it to ob
	 *
	 * @param data InjectData instance
	 * @param name The name of the field you want to find
	 * @param classHint The name of the class you expect the field to be in, or null
	 * @param type The type the method has in deob, or null
	 *
	 * @return The obfuscated version of the found field
	 */
	static Field findStaticField(InjectData data, String name, String classHint, Type type) throws Injexception
	{
		final ClassGroup deob = data.getDeobfuscated();
		Field field;

		if (classHint != null)
		{
			ClassFile clazz = findClassOrThrow(deob, classHint);

			if (type == null)
			{
				field = clazz.findField(name);
			}
			else
			{
				field = clazz.findField(name, type);
			}

			if (field != null)
			{
				return data.toVanilla(field);
			}
		}

		for (ClassFile clazz : deob)
		{
			if (type == null)
			{
				field = clazz.findField(name);
			}
			else
			{
				field = clazz.findField(name, type);
			}

			if (field != null)
			{
				return data.toVanilla(field);
			}
		}

		throw new Injexception(String.format("Static field %s doesn't exist", (type != null ? type + " " : "") + name));
	}

	/**
	 * Finds a static field in deob and converts it to ob
	 *
	 * @param data InjectData instance
	 * @param pool Pool field of the field you want
	 *
	 * @return The obfuscated version of the found field
	 */
	static Field findStaticField(InjectData data, net.runelite.asm.pool.Field pool) throws Injexception
	{
		return findStaticField(data, pool.getName(), pool.getClazz().getName(), pool.getType());
	}

	/**
	 * Fail-fast implementation of ClassGroup.findFieldDeep
	 */
	static Field findFieldDeep(ClassFile clazz, String name) throws Injexception
	{
		do
		{
			Field f = clazz.findField(name);
			if (f != null)
			{
				return f;
			}
		}
		while ((clazz = clazz.getParent()) != null);
		throw new Injexception("Couldn't find field " + name);
	}

	static Field findField(InjectData data, String name, String hintClass) throws Injexception
	{
		final ClassGroup deob = data.getDeobfuscated();
		return data.toVanilla(findField(deob, name, hintClass));
	}

	static Field findField(ClassGroup group, String name, String hintClass) throws Injexception
	{
		Field field;
		if (hintClass != null)
		{
			ClassFile clazz = findClassOrThrow(group, hintClass);

			field = clazz.findField(name);
			if (field != null)
			{
				return field;
			}
		}

		for (ClassFile clazz : group)
		{
			field = clazz.findField(name);
			if (field != null)
			{
				return field;
			}
		}

		throw new Injexception("Field " + name + " doesn't exist");
	}

	static ClassFile fromApiMethod(InjectData data, RSApiMethod apiMethod)
	{
		return data.toVanilla(data.toDeob(apiMethod.getClazz().getName()));
	}

	static Signature apiToDeob(InjectData data, Signature api)
	{
		return new Signature.Builder()
			.setReturnType(apiToDeob(data, api.getReturnValue()))
			.addArguments(
				api.getArguments().stream()
					.map(type -> apiToDeob(data, type))
					.collect(Collectors.toList())
			).build();
	}

	static Type apiToDeob(InjectData data, Type api)
	{
		if (api.isPrimitive())
		{
			return api;
		}

		final String internalName = api.getInternalName();
		if (internalName.startsWith(API_BASE))
		{
			return Type.getType("L" + api.getInternalName().substring(API_BASE.length()) + ";", api.getDimensions());
		}
		else if (internalName.startsWith(RL_API_BASE))
		{
			Class rlApiC = new Class(internalName);
			RSApiClass highestKnown = data.getRsApi().withInterface(rlApiC);

			// Cheeky unchecked exception
			assert highestKnown != null : "No rs api class implements rl api class " + rlApiC.toString();

			boolean changed;
			do
			{
				changed = false;

				for (RSApiClass interf : highestKnown.getApiInterfaces())
				{
					if (interf.getInterfaces().contains(rlApiC))
					{
						highestKnown = interf;
						changed = true;
						break;
					}
				}
			}
			while (changed);

			return apiToDeob(data, Type.getType(highestKnown.getName(), api.getDimensions()));
		}

		return api;
	}

	static Type deobToVanilla(InjectData data, Type deobT)
	{
		if (deobT.isPrimitive())
		{
			return deobT;
		}

		final ClassFile deobClass = data.getDeobfuscated().findClass(deobT.getInternalName());
		if (deobClass == null)
		{
			return deobT;
		}

		return Type.getType("L" + data.toVanilla(deobClass).getName() + ";", deobT.getDimensions());
	}

	static boolean apiToDeobSigEquals(InjectData data, Signature deobSig, Signature apiSig)
	{
		return deobSig.equals(apiToDeob(data, apiSig));
	}

	static boolean argsMatch(Signature a, Signature b)
	{
		List<Type> aa = a.getArguments();
		List<Type> bb = b.getArguments();

		if (aa.size() != bb.size())
		{
			return false;
		}

		for (int i = 0; i < aa.size(); i++)
		{
			if (!aa.get(i).equals(bb.get(i)))
			{
				return false;
			}
		}

		return true;
	}

	/**
	 * Gets the obfuscated name from something's annotations.
	 *
	 * If the annotation doesn't exist return the current name instead.
	 */
	static <T extends Annotated & Named> String getObfuscatedName(T from)
	{
		Annotation name = from.getAnnotations().find(DeobAnnotations.OBFUSCATED_NAME);
		return name == null ? from.getName() : name.getElement().getString();
	}

	/**
	 * Gets the value of the @Export annotation on the object.
	 */
	static String getExportedName(Annotated from)
	{
		Annotation export = from.getAnnotations().find(DeobAnnotations.EXPORT);
		return export == null ? null : export.getElement().getString();
	}

	/**
	 * Creates the correct load instruction for the variable with Type type and var index index.
	 */
	static Instruction createLoadForTypeIndex(Instructions instructions, Type type, int index)
	{
		if (type.getDimensions() > 0 || !type.isPrimitive())
		{
			return new ALoad(instructions, index);
		}

		switch (type.toString())
		{
			case "B":
			case "C":
			case "I":
			case "S":
			case "Z":
				return new ILoad(instructions, index);
			case "D":
				return new DLoad(instructions, index);
			case "F":
				return new FLoad(instructions, index);
			case "J":
				return new LLoad(instructions, index);
			default:
				throw new IllegalStateException("Unknown type");
		}
	}

	/**
	 * Creates the right return instruction for an object with Type type
	 */
	static Instruction createReturnForType(Instructions instructions, Type type)
	{
		if (!type.isPrimitive())
		{
			return new Return(instructions, InstructionType.ARETURN);
		}

		switch (type.toString())
		{
			case "B":
			case "C":
			case "I":
			case "S":
			case "Z":
				return new Return(instructions, InstructionType.IRETURN);
			case "D":
				return new Return(instructions, InstructionType.DRETURN);
			case "F":
				return new Return(instructions, InstructionType.FRETURN);
			case "J":
				return new Return(instructions, InstructionType.LRETURN);
			case "V":
				return new VReturn(instructions);
			default:
				throw new IllegalStateException("Unknown type");
		}
	}

	static Instruction createInvokeFor(Instructions instructions, net.runelite.asm.pool.Method method, boolean isStatic)
	{
		if (isStatic)
		{
			return new InvokeStatic(instructions, method);
		}
		else
		{
			return new InvokeVirtual(instructions, method);
		}
	}

	/**
	 * Legit fuck annotations
	 */
	static ClassFile getVanillaClassFromAnnotationString(InjectData data, Annotation annotation)
	{
		Object v = annotation.getElement().getValue();
		String str = ((org.objectweb.asm.Type) v).getInternalName();
		return data.toVanilla(data.toDeob(str));
	}
}