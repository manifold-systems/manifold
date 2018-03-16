$(function(){
  // init gif player
  $('.lazygif').gifplayer({ label: '►' });

  // fix menu scrolling
  $('.docs-toc').on('click', 'a[href^="#"]', function (event) {
      event.preventDefault();
      var href = $.attr(this, 'href');
      var offset = $(href).offset().top - 75;
      window.location.hash = href;
      $('html, body').animate({scrollTop: offset}, 50);
  });

    $(window).scroll(function(){
      var scrollTop = $(this).scrollTop() + 100;
      var closestAboveFold = null;
      $("h2, h3").each(function() {
        if($(this).offset().top < scrollTop) {
          if(closestAboveFold && closestAboveFold.offset().top < $(this).offset().top) {
            closestAboveFold = $(this);
          } else {
            closestAboveFold = $(this);
          }
        }
      });

      $('.docs-toc').find('a').removeClass('active');

      if(closestAboveFold) {
        var anchorSelector = 'a[href="#' + closestAboveFold.attr('id') + '"]';
        var active = $('.docs-toc').find(anchorSelector);
        active.addClass("active");
        active[0].scrollIntoView();
      }
    })

});