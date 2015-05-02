package idp;

import basic.FileUtil;
import basic.StringUtil;
import idp.program.EntailsProgram;
import idp.program.IdpProgram;
import idp.program.ValidityProgram;
import log.Log;
import logic.theory.InlineTheory;
import logic.theory.KnowledgeBase;
import logic.theory.LogicExecutor;
import runtime.Terminal;
import time.Stopwatch;
import vector.Vector;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by samuelkolb on 11/11/14.
 *
 * @author Samuel Kolb
 */
public class IdpExecutor implements LogicExecutor {

	//region Variables
	private static IdpExecutor executor;

	/**
	 * Returns the singleton executor instance
	 * @return	An idp executor
	 */
	public static IdpExecutor get() {
		if(executor == null)
			executor = new IdpExecutor();
		return executor;
	}

	private final Terminal terminal = Terminal.get();

	public Terminal getTerminal() {
		return terminal;
	}

	public final Stopwatch entailmentStopwatch = new Stopwatch();

	public int entailmentCount = 0;

	public int noEntailmentCount = 0;

	// IVAR fileManager - The file manager used for temporary files

	private final FileManager fileManager;

	protected FileManager getFileManager() {
		return fileManager;
	}

	//endregion

	//region Construction
	private IdpExecutor() {
		this.fileManager = new FileManager("temp");
		getFileManager().cleanTempDir("idp");
	}
	//endregion

	//region Public methods

	@Override
	public List<Vector<Boolean>> testValidityTheories(KnowledgeBase knowledgeBase) {
		String result = executeSafe(new ValidityProgram(knowledgeBase));
		List<Vector<Boolean>> list = new ArrayList<>();
		for(String line : result.trim().split("\n"))
			if(line.trim().length() > 0)
				list.add(new Vector<>(line.trim().split(" ")).map(Boolean.class, this::getBoolean));
		return list;
	}

	@Override
	public boolean entails(KnowledgeBase knowledgeBase, InlineTheory theory) {
		entailmentStopwatch.start();
		boolean test = executeTest(new EntailsProgram(knowledgeBase, theory));
		entailmentStopwatch.pause();
		entailmentCount++;
		if(!test)
			noEntailmentCount++;
		return test;
	}

	private boolean[] executeTests(IdpProgram idpProgram) throws IllegalStateException {
		String[] lines = executeSafe(idpProgram).trim().split("\n");
		boolean[] result = new boolean[lines.length];
		for(int i = 0; i < result.length; i++)
			result[i] = getBoolean(lines[i]);
		return result;
	}

	private boolean executeTest(IdpProgram idpProgram) throws IllegalStateException {
		//Log.LOG.printLine(getDebugString(idpProgram));
		//Log.LOG.printTitle("Program");
		//Log.LOG.printLine(idpProgram.print()).newLine().newLine();
		String string = executeSafe(idpProgram).trim();
		try {
			return getBoolean(string);
		} catch(Exception e) {
			Log.LOG.printTitle("Error occurred:").printLine(e.getMessage()).newLine();
			Log.LOG.printTitle("Program:").printLine(getDebugString(idpProgram));
			throw e;
		}
	}

	private boolean getBoolean(String string) {
		if("YES".equals(string))
			return true;
		if("NO".equals(string))
			return false;
		throw new IllegalStateException("Incorrect output: " + string);
	}

	private String executeSafe(IdpProgram idpProgram) throws IllegalStateException {
		try {
			return execute(idpProgram);
		} catch(Exception e) {
			e.printStackTrace();
			Log.LOG.printTitle("Error occurred:").printLine(e.getMessage()).newLine();
			Log.LOG.printTitle("Program:").printLine(getDebugString(idpProgram));
			throw new IllegalStateException(e);
		}
	}

	private String getDebugString(IdpProgram idpProgram) {
		String string = idpProgram.print();
		String[] lines = string.split("\n");
		if(lines.length == 0)
			return "";
		int digits = (int) Math.floor(Math.log10(lines.length)) + 1;
		for(int i = 0; i < lines.length; i++)
			lines[i] = printFixed(i+1, digits) + "| " + lines[i];
		return StringUtil.join("\n", (Object[]) lines);
	}

	private String printFixed(int number, int length) {
		int digits = (int) Math.floor(Math.log10(number) + 1);
		return StringUtil.getRepeated(' ', length - digits) + number;
	}

	/**
	 * Executes an idp program and returns the result
	 * @param program	The program to run
	 * @return	The output of idp
	 */
	public String execute(IdpProgram program) {
		return execute(program.print());
	}

	/**
	 * Executes an idp program and returns the result
	 * @param idpProgram	The name of the idp program file
	 * @return	The output of idp
	 */
	public String execute(String idpProgram) {
		File idpFile = createFile(idpProgram);
		return execute(idpFile);
	}

	/**
	 * Executes an idp program and returns the result
	 * @param file	The file containing an idp program
	 * @return	The output of idp
	 */
	public String execute(File file) {
		String idpPath = FileUtil.getLocalFile(getClass().getResource("/executable/mac/idp/bin/idp")).getAbsolutePath();
		String filePath = file.getAbsolutePath();
		String command = idpPath + " " + filePath;
		String result = getTerminal().runCommand(command);
		getTerminal().execute("unlink " + file.getAbsolutePath(), true);
		return result;
	}

	private File createFile(final String string) {
		final File file = getFileManager().createRandomFile("idp");
		getTerminal().execute("mkfifo " + file.getAbsolutePath(), true);
		new Thread(() -> {
			PrintWriter writer = null;
			try {
				writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));
				writer.write(string);
			} catch(IOException e) {
				throw new IllegalStateException("Unexpected error.", e);
			} finally {
				if(writer != null)
					writer.close();
			}
		}).start();
		return file;
	}
	//endregion
}
