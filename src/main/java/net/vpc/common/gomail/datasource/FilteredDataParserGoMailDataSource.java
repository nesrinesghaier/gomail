/*
 * To change this license header, choose License Headers in Project Properties.
 *
 * and open the template in the editor.
 */
package net.vpc.common.gomail.datasource;

import java.util.ArrayList;
import java.util.List;
import net.vpc.common.gomail.GoMailContext;
import net.vpc.common.gomail.GoMailDataSource;
import net.vpc.common.gomail.GoMailDataSourceRow;
import net.vpc.common.gomail.modules.GoMailModuleSerializer;
import net.vpc.common.gomail.util.SerializedForm;
import net.vpc.upa.QLExpressionParser;
import net.vpc.upa.UPA;
import net.vpc.upa.expressions.Select;

/**
 *
 * @author taha.bensalah@gmail.com
 */
public class FilteredDataParserGoMailDataSource extends AbstractGoMailDataSource {

    private List<GoMailDataSourceRow> rows = new ArrayList<GoMailDataSourceRow>();
    private String[] sColumns;
    private boolean parsed = false;
    private GoMailDataSourceFilter filter;
    private GoMailDataSource dataSource;
//    private Map<String, XMailDataSource> dataSources;

    protected FilteredDataParserGoMailDataSource(String source) {
        super(source);
//        this.filter = filter;
//        this.dataSources = dataSources;
    }

    @Override
    public SerializedForm serialize() {
//        XMailDataSource ss = (XMailDataSource) getSource();
        //"'" + ss.serialize() + "' where " + filter.serialize()
        return new SerializedForm(getClass().getName(), getSource().toString());
    }

    public static FilteredDataParserGoMailDataSource valueOf(SerializedForm s) {
        return new FilteredDataParserGoMailDataSource(s.getValue());

//        return new FilteredDataParserXMailDataSource(new HashMap<String, XMailDataSource>(), ds, dsf);
    }

    @Override
    public void build(GoMailContext context) {
        super.build(context);

        String ss = (String) getBuildSource();
        String[] aa = ss.split("( |\t|\n|\r)");
        if (!aa[0].toLowerCase().equals("select")) {
            boolean hasFrom = false;
            for (int i = 0; i < aa.length; i++) {
                if (aa[i].toLowerCase().equals("from")) {
                    hasFrom = true;
                    break;
                }
            }
            if (hasFrom) {
                ss = "select * " + ss;
            } else {
                ss = "select * from " + ss;
            }
        }
        QLExpressionParser parser = UPA.getBootstrap().getFactory().createObject(QLExpressionParser.class);
        Select expr = (Select) parser.parse(ss);
        String en = expr.getEntityName();
        String ea = expr.getEntityAlias();
        dataSource = null;
        if (en == null && ea != null) {
            dataSource = context.getRegisteredDataSources().get(ea);
            if (dataSource == null) {
                dataSource = GoMailModuleSerializer.deserializeDataSource(ea);
            }
        } else if (en != null && ea == null) {
            dataSource = context.getRegisteredDataSources().get(en);
            if (dataSource == null) {
                dataSource = GoMailModuleSerializer.deserializeDataSource(en);
            }
        } else {
            dataSource = GoMailModuleSerializer.deserializeDataSource(en, ea);
        }
//        expr.from((NameOrSelect) null, null);
        filter = GoMailModuleSerializer.deserializeDataSourceFilter(expr.toString());
    }

    private void parse() {
        if (!parsed) {
            parsed = true;
            GoMailDataSource data = dataSource;

            String[] columns = data.getColumns();

            String[] colString = new String[columns.length];
            for (int i = 0; i < colString.length; i++) {
                colString[i] = columns[i];
            }
            sColumns = colString;
            int max = data.getRowCount();
            for (int i = 0; i < max; i++) {
                GoMailDataSourceRow r = data.getRow(i);
                if (filter.accept(getContext(), r)) {
                    rows.add(r);
                }
            }
        }
    }

    @Override
    public String getCell(int rowIndex, int colIndex) {
        parse();
        return rows.get(rowIndex).get(colIndex);
    }

    @Override
    public int getColumnCount() {
        parse();
        return sColumns.length;
    }

    @Override
    public String[] getColumns() {
        parse();
        return sColumns;
    }

    @Override
    public int getRowCount() {
        parse();
        return rows.size();
    }

}
