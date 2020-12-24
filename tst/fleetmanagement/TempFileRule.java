package fleetmanagement;

import java.io.*;

import org.apache.commons.io.FileUtils;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

@SuppressWarnings("serial")
public class TempFileRule extends TempFile implements TestRule {

    public TempFileRule(String name) {
        super(name);
    }

    public TempFileRule() {}

    @Override
    protected void finalize() throws Throwable {
        delete();
        super.finalize();
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return this.statement(base);
    }

    private Statement statement(final Statement base) {
        return new Statement() {
            public void evaluate() throws Throwable {
                TempFileRule.this.before();

                try {
                    base.evaluate();
                } finally {
                    TempFileRule.this.after();
                }

            }
        };
    }

    protected void before() throws Throwable {
    }

    protected void after() {
        delete();
    }
}
