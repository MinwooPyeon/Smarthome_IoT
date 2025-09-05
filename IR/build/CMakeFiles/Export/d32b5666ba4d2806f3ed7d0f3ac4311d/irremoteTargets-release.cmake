#----------------------------------------------------------------
# Generated CMake target import file for configuration "Release".
#----------------------------------------------------------------

# Commands may need to know the format version.
set(CMAKE_IMPORT_FILE_VERSION 1)

# Import target "irremote_lib" for configuration "Release"
set_property(TARGET irremote_lib APPEND PROPERTY IMPORTED_CONFIGURATIONS RELEASE)
set_target_properties(irremote_lib PROPERTIES
  IMPORTED_LINK_INTERFACE_LANGUAGES_RELEASE "CXX"
  IMPORTED_LOCATION_RELEASE "${_IMPORT_PREFIX}/lib/libirremote_lib.a"
  )

list(APPEND _cmake_import_check_targets irremote_lib )
list(APPEND _cmake_import_check_files_for_irremote_lib "${_IMPORT_PREFIX}/lib/libirremote_lib.a" )

# Import target "irremote_sender" for configuration "Release"
set_property(TARGET irremote_sender APPEND PROPERTY IMPORTED_CONFIGURATIONS RELEASE)
set_target_properties(irremote_sender PROPERTIES
  IMPORTED_LOCATION_RELEASE "${_IMPORT_PREFIX}/bin/irremote_sender.exe"
  )

list(APPEND _cmake_import_check_targets irremote_sender )
list(APPEND _cmake_import_check_files_for_irremote_sender "${_IMPORT_PREFIX}/bin/irremote_sender.exe" )

# Commands beyond this point should not need to know the version.
set(CMAKE_IMPORT_FILE_VERSION)
