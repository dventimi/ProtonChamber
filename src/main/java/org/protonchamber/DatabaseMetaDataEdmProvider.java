package org.protonchamber;

import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import javax.naming.*;
import javax.sql.*;
import org.apache.olingo.commons.api.edm.*;
import org.apache.olingo.commons.api.edm.provider.*;
import org.apache.olingo.commons.api.ex.*;

public class DatabaseMetaDataEdmProvider extends CsdlAbstractEdmProvider {
    protected DatabaseMetaData m;

    interface Processor {
	void process (ResultSet r) throws SQLException;}

    static class ProtonRoot implements Processor {
	Map<String, ProtonSchema> schemas = new HashMap<>();
	public ProtonRoot () {}
	@Override
	public void process (ResultSet r) throws SQLException {
	    schemas.putIfAbsent(r.getString("TABLE_SCHEM"), new ProtonSchema(r));
	    for (Processor p : schemas.values()) p.process(r);}
	public List<CsdlSchema> getSchemas () {
	    return new ArrayList<>(schemas.values());}}

    static class ProtonSchema extends CsdlSchema implements Processor {
	Map<String, ProtonEntityType> entityTypes = new HashMap<>();
	Map<String, ProtonEntityContainer> entityContainers = new HashMap<>();
	public ProtonSchema (ResultSet r) throws SQLException {
	    super();
	    setNamespace(r.getString("TABLE_SCHEM"));}
	@Override
	public void process (ResultSet r) throws SQLException {
	    if (r.getString("TABLE_SCHEM").equals(getNamespace())) entityTypes.putIfAbsent(r.getString("TABLE_NAME"), new ProtonEntityType(r));
	    if (r.getString("TABLE_SCHEM").equals(getNamespace())) entityContainers.putIfAbsent("entityContainer", new ProtonEntityContainer(this, r));
	    for (Processor p : entityTypes.values()) p.process(r);
	    for (Processor p : entityContainers.values()) p.process(r);}
	@Override
	public List<CsdlEntityType> getEntityTypes () {
	    return new ArrayList<>(entityTypes.values());}
	@Override
	public CsdlEntityContainer getEntityContainer () {
	    for (CsdlEntityContainer e : entityContainers.values()) return e;
	    throw new IllegalStateException();}}

    static class ProtonEntityType extends CsdlEntityType implements Processor {
	public ProtonEntityType (ResultSet r) throws SQLException {
	    super();
	    setName(r.getString("TABLE_NAME"));}
	@Override
	public void process (ResultSet r) {}}

    static class ProtonEntityContainer extends CsdlEntityContainer implements Processor {
	CsdlSchema schema;
	Map<String, ProtonEntitySet> entitySets = new HashMap<>();
	public ProtonEntityContainer (CsdlSchema schema, ResultSet r) throws SQLException {
	    super();
	    this.schema = schema;
	    setName("EntityContainer");}
	public CsdlSchema getSchema () {
	    return schema;}
	@Override
	public void process (ResultSet r) throws SQLException {
	    if (r.getString("TABLE_SCHEM").equals(getSchema().getNamespace())) entitySets.putIfAbsent(r.getString("TABLE_NAME"), new ProtonEntitySet(r));
	    for (Processor p : entitySets.values()) p.process(r);}
	@Override
	public List<CsdlEntitySet> getEntitySets () {
	    return new ArrayList<>(entitySets.values());}}

    static class ProtonEntitySet extends CsdlEntitySet implements Processor {
	public ProtonEntitySet (ResultSet r) throws SQLException {
	    super();
	    setName(r.getString("TABLE_NAME"));
	    setType(new FullQualifiedName(r.getString("TABLE_SCHEM"), r.getString("TABLE_NAME")));}
	@Override
	public void process (ResultSet r) {}}
    
    public DatabaseMetaDataEdmProvider (DatabaseMetaData m) {
	super();
	this.m = m;}

    @Override
    public List<CsdlSchema> getSchemas () throws ODataException {
	ProtonRoot root = new ProtonRoot();
	try (ResultSet r = m.getColumns(null, null, null, null)) {
	    while (r.next()) root.process(r);
	    return root.getSchemas();}
	catch (Throwable e) {e.printStackTrace(System.out); throw new ODataException(e);}}
}
