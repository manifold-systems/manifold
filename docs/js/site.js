// polyfill
if (!Element.prototype.scrollIntoViewIfNeeded) {
  Element.prototype.scrollIntoViewIfNeeded = function (centerIfNeeded) {
    centerIfNeeded = arguments.length === 0 ? true : !!centerIfNeeded;

    var parent = this.parentNode,
        parentComputedStyle = window.getComputedStyle(parent, null),
        parentBorderTopWidth = parseInt(parentComputedStyle.getPropertyValue('border-top-width')),
        parentBorderLeftWidth = parseInt(parentComputedStyle.getPropertyValue('border-left-width')),
        overTop = this.offsetTop - parent.offsetTop < parent.scrollTop,
        overBottom = (this.offsetTop - parent.offsetTop + this.clientHeight - parentBorderTopWidth) > (parent.scrollTop + parent.clientHeight),
        overLeft = this.offsetLeft - parent.offsetLeft < parent.scrollLeft,
        overRight = (this.offsetLeft - parent.offsetLeft + this.clientWidth - parentBorderLeftWidth) > (parent.scrollLeft + parent.clientWidth),
        alignWithTop = overTop && !overBottom;

    if ((overTop || overBottom) && centerIfNeeded) {
      parent.scrollTop = this.offsetTop - parent.offsetTop - parent.clientHeight / 2 - parentBorderTopWidth + this.clientHeight / 2;
    }

    if ((overLeft || overRight) && centerIfNeeded) {
      parent.scrollLeft = this.offsetLeft - parent.offsetLeft - parent.clientWidth / 2 - parentBorderLeftWidth + this.clientWidth / 2;
    }

    if ((overTop || overBottom || overLeft || overRight) && !centerIfNeeded) {
      this.scrollIntoView(alignWithTop);
    }
  };
}

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
        active[0].scrollIntoViewIfNeeded();
      }
    })

});