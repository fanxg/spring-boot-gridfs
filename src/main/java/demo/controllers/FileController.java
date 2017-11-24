package demo.controllers;

import com.mongodb.gridfs.GridFSDBFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsCriteria;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/files")
public class FileController {

  private final GridFsTemplate gridFsTemplate;

  @Autowired
  public FileController(GridFsTemplate gridFsTemplate) {
    this.gridFsTemplate = gridFsTemplate;
  }

  @RequestMapping(method = RequestMethod.POST)
  public HttpEntity<byte[]> createOrUpdate(@RequestParam("file") MultipartFile file) {
    String name = file.getOriginalFilename();
    try {
      Optional<GridFSDBFile> existing = maybeLoadFileByName(name);
      if (existing.isPresent()) {
        gridFsTemplate.delete(getFilenameQuery(name));
      }
      gridFsTemplate.store(file.getInputStream(), name, file.getContentType()).save();
      String resp = "<script>window.location = '/';</script>";
      return new HttpEntity<>(resp.getBytes());
    } catch (IOException e) {
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @RequestMapping(method = RequestMethod.GET)
  public @ResponseBody List<String> list() {
    return getFiles().stream()
        .map(GridFSDBFile::getFilename)
        .collect(Collectors.toList());
  }

  @GetMapping("getIds")
  public @ResponseBody List<String> ids() {
    return getFiles().stream()
        .map(e -> e.getId().toString())
        .collect(Collectors.toList());
  }

  @GetMapping("deleteAll")
  public void deleteAll(){
    gridFsTemplate.delete(null);
  }

  @RequestMapping(path = "/{name:.+}", method = RequestMethod.GET)
  public HttpEntity<byte[]> get(@PathVariable("name") String name) {
    Optional<GridFSDBFile> optionalCreated = maybeLoadFileByName(name);
    if (optionalCreated.isPresent()) {
      return getheepEntity(optionalCreated);
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @RequestMapping(path = "/id/{id:.+}", method = RequestMethod.GET)
  public HttpEntity<byte[]> getById(@PathVariable("id") String id) {
    Optional<GridFSDBFile> optionalCreated = maybeLoadFileById(id);
    if (optionalCreated.isPresent()) {
      return getheepEntity(optionalCreated);
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  private HttpEntity getheepEntity(Optional<GridFSDBFile> optionalCreated) {
    GridFSDBFile created = optionalCreated.get();
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    try {
      created.writeTo(os);
      HttpHeaders headers = new HttpHeaders();
      headers.add(HttpHeaders.CONTENT_TYPE, created.getContentType());
      return new HttpEntity<>(os.toByteArray(), headers);
    } catch (IOException e) {
      return new ResponseEntity<>(HttpStatus.IM_USED);
    }
  }

  private List<GridFSDBFile> getFiles() {
    return gridFsTemplate.find(null);
  }

  private Optional<GridFSDBFile> maybeLoadFileByName(String name) {
    GridFSDBFile file = gridFsTemplate.findOne(getFilenameQuery(name));
    return Optional.ofNullable(file);
  }

  private static Query getFilenameQuery(String name) {
    return Query.query(GridFsCriteria.whereFilename().is(name));
  }

  private Optional<GridFSDBFile> maybeLoadFileById(String id) {
    GridFSDBFile file = gridFsTemplate.findOne(getFileIdQuery(id));
    return Optional.ofNullable(file);
  }

  private Query getFileIdQuery(String id) {
    return Query.query(GridFsCriteria.where("_id").is(id));
  }
}
