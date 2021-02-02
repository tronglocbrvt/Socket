$(document).ready(function(){
   $("button").click(function(e){
      var name = $(this).parent().parent().find("a").first().attr("href");
      $.post("files", name)});
});