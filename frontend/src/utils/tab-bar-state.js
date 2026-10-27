let activeTabPath = ''

export function getActiveTabPath() {
  return activeTabPath
}

export function setActiveTabPath(path) {
  activeTabPath = path

  if (typeof uni !== 'undefined' && typeof uni.$emit === 'function') {
    uni.$emit('tea-tab-bar-active-path', path)
  }
}
